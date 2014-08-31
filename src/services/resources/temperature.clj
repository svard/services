(ns services.resources.temperature
  (:require [clojure.string :as string]
            [liberator.core :refer [defresource]]
            [monger.core :as mongo]
            [monger.collection :as coll]
            [monger.query :as query]
            [monger.result :as result]
            [monger.joda-time]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [taoensso.timbre :as timbre]
            [services.utils.date :as date]
            [services.utils.math :as math])
  (:import [org.bson.types ObjectId]))

(timbre/refer-timbre)

(def conn (mongo/connect {:host "192.168.0.108" :port 27017}))
(def db (mongo/get-db conn "temperature_db"))
(def collection "temperatures")
(def temp-transducer (map #(:temperature %)))
(def time-format (f/formatter "yyyy-MM-dd HH:mm:ss"))

(defn- get-temp-range
  ([]
    (query/with-collection db collection
      (query/find {})
      (query/fields [:temperature :dateStr :date :deviceId :deviceName])))
  ([range-fn]
    (let [[start end] (range-fn)]
      (query/with-collection db collection
        (query/find {:date {"$lt" end, "$gte" start}})
        (query/fields [:temperature :dateStr :date :deviceId :deviceName])))))

(defn- mean-temp [xs]
  (math/round (/ (transduce temp-transducer + 0 xs) (count xs))))

(defn- prepare-temp-response
  ([]
    (map #(update-in % [:_id] str) (get-temp-range)))
  ([year]
    (let [range-fn (partial date/get-year-start-end-timestamp year)]
      (map #(update-in % [:_id] str) (get-temp-range range-fn))))
  ([year month]
     (let [range-fn (partial date/get-month-start-end-timestamp year month)]
      (map #(update-in % [:_id] str) (get-temp-range range-fn)))))

(defn- get-resource [params]
  (cond
    (and (:year params) (:month params)) {:entity (prepare-temp-response (read-string (:year params))
                                                                         (read-string (:month params)))}
    (:year params) {:entity (prepare-temp-response (read-string (:year params)))}
    (and (:month params) (nil? (:year params))) false
    :else {:entity (prepare-temp-response)}))

(defn- store-temp [temp-map]
  (let [id (ObjectId.)
        now (l/local-now)
        write-result (->> (assoc temp-map :_id id :d now)
                          (coll/insert db collection))]
    (if (result/ok? write-result)
      (str id)
      (throw Exception))))

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
  :allowed-methods [:get :post]
  :exists? (fn [_]
             (get-resource params))
  :handle-ok :entity
  :post! (fn [ctx]
           (let [body (get-in ctx [:request :body-params])]
             (info (str "Storing temp " body))
             {:location (str "/api/temperature/" (store-temp body))})))
