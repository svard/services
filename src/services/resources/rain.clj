(ns services.resources.rain
  (:require [liberator.core :refer [defresource]]
            [clojure.data.json :as json]
            [monger.core :as mongo]
            [monger.collection :as coll]
            [monger.query :as query]
            [monger.joda-time]
            [clj-time.local :as l]
            [taoensso.timbre :as timbre])
  (:import [org.bson.types ObjectId]))

(timbre/refer-timbre)

(def conn (mongo/connect {:host "192.168.0.108" :port 27017}))
(def db (mongo/get-db conn "services_db"))
(def collection "rainrates")

(defn aggregate-rain
  ([]
      (coll/aggregate db "rainrates" [{"$project" {:year {"$year" "$date"} :month {"$month" "$date"} :rain "$rainrate"}}
                                     {"$group" {:_id {:year "$year" :month "$month"} :total {"$sum" "$rain"}}}
                                     {"$project" {:_id 0 :year "$_id.year" :month "$_id.month" :total "$total"}}]))
  ([year]
     (coll/aggregate db "rainrates" [{"$project" {:year {"$year" "$date"} :month {"$month" "$date"} :rain "$rainrate"}}
                                     {"$match" {:year year}}
                                     {"$group" {:_id {:year "$year" :month "$month"} :total {"$sum" "$rain"}}}
                                     {"$project" {:_id 0 :month "$_id.month" :total "$total"}}]))
  ([year month]
     (coll/aggregate db "rainrates" [{"$project" {:year {"$year" "$date"} :month {"$month" "$date"} :rain "$rainrate"}}
                                     {"$match" {:year year :month month}}
                                     {"$group" {:_id 0 :total {"$sum" "$rain"}}}])))

(defn- get-resource [params]
  (let [year (:year params)
        month (:month params)]
    (cond
     (and year month) {:entity (aggregate-rain (read-string year) (read-string month))}
     (and month (nil? year)) false
     (not (nil? year)) {:entity (aggregate-rain (read-string year))}
     :else {:entity (aggregate-rain)})))

(defn- store-rain [rain-map]
  (let [id (ObjectId.)
        now (l/local-now)]
    (->> (assoc rain-map :_id id :date now)
         (coll/insert db collection))))

(defn handle-rain-measure [ch metadata ^bytes payload]
  (let [measure (json/read-str (String. payload "UTF-8") :key-fn keyword)]
    (info "Storing rain" measure)
    (store-rain measure)))

(defresource rain [{:keys [params] :as req}]
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :exists? (fn [_]
             (get-resource params))
  :handle-ok :entity)
