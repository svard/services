(ns services.resources.temperature
  (:require [clojure.string :as string]
            [liberator.core :refer [defresource]]
            [monger.core :as mongo]
            [monger.collection :as coll]
            [monger.query :as query]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [services.utils.date :as date]
            [services.utils.math :as math]))

(def conn (mongo/connect {:host "192.168.0.108" :port 27017}))
(def db (mongo/get-db conn "services_db"))
(def temp (map #(:temperature %)))

(defn- get-temp-range
  ([]
    (query/with-collection db "temperatures"
      (query/find {})
      (query/fields [:temperature :dateStr :date :deviceId :deviceName])))
  ([range-fn]
    (let [[start end] (range-fn)]
      (query/with-collection db "temperatures"
        (query/find {:date {"$lt" end, "$gte" start}})
        (query/fields [:temperature :dateStr :date :deviceId :deviceName])))))

(defn- mean-temp [xs]
  (math/round (/ (transduce temp + 0 xs) (count xs))))

(defn- prepare-temp-response
  ([]
    (map #(update-in % [:_id] str) (get-temp-range)))
  ([year]
    (let [range-fn (partial date/get-year-start-end-timestamp year)]
      (map #(update-in % [:_id] str) (get-temp-range range-fn))))
  ([year month]
     (let [range-fn (partial date/get-month-start-end-timestamp year month)]
      (map #(update-in % [:_id] str) (get-temp-range range-fn)))))

(defresource current [req]
  :available-media-types ["application/json"]
  :handle-ok (fn [_]
               (let [today (date/today)
                     range-fn (partial date/get-date-start-end-timestamp today today)
                     data (get-temp-range range-fn)
                     max (apply max-key :temperature data)
                     min (apply min-key :temperature data)]
                 {:max (:temperature max)
                  :maxTime (-> (:dateStr max)
                               (string/split #"\s")
                               (last))
                  :min (:temperature min)
                  :minTime (-> (:dateStr min)
                               (string/split #"\s")
                               (last))
                  :mean (mean-temp data)
                  :current (:temperature (last data))
                  :dateStr (:dateStr (last data))
                  :deviceId (:deviceId (last data))
                  :deviceName (:deviceName (last data))})))

(defresource temperature [{:keys [params] :as req}]
  :available-media-types ["application/json"]
  :handle-ok (fn [_]
               (cond
                 (and (:year params) (:month params)) (prepare-temp-response (read-string (:year params)) (read-string (:month params)))
                 (:year params) (prepare-temp-response (read-string (:year params)))
                 (and (:month params) (nil? (:year params))) {}
                 :else (prepare-temp-response))))
