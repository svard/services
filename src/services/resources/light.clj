(ns services.resources.light
  (:require [liberator.core :refer [defresource]]
            [clojure.string :as string]
            [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]]
            [monger.core :as mongo]
            [monger.collection :as coll]
            [monger.joda-time]
            [clj-time.local :as l]
            [taoensso.carmine :as car :refer (wcar)]
            [taoensso.timbre :as timbre]
            [services.message-bus :as mb])
  (:import [org.bson.types ObjectId]))

(timbre/refer-timbre)

(def conn (mongo/connect {:host "192.168.0.108" :port 27017}))
(def db (mongo/get-db conn "temperature_db"))
(def collection "lights")
(def redis-conn {:spec {:host "192.168.0.108" :port 6379}})

(defmacro wcar*
  [& body]
  `(car/wcar redis-conn ~@body))

(defn- redis->map [xs]
  (reduce #(assoc %1 (keyword (first %2)) (last %2))
          {}
          (partition 2 xs)))

(defn- find-one [id]
  (redis->map (wcar* (car/hgetall (str "light:" id)))))

(defn- switch [id state]
  (info "Executing command:" "tdtool" (str "--" (string/lower-case state)) (str id))
  (sh "tdtool" (str "--" (string/lower-case state)) (str id)))

(defn cache-light-event [event]
  (let [id (:id event)
        state (:state event)]
    (wcar* (car/hmset (str "light:" id) "state" state))))

(defn- store-light-event [event]
  (let [id (ObjectId.)]
    (->> (assoc event :_id id)
         (coll/insert db collection))))

(defn- publish-light-event [event]
  (mb/publish "automation" "light" (json/write-str event)))

(defresource lights [req]
  :available-media-types ["application/json"]
  :allowed-methods [:get :put]
  :handle-ok (map find-one [1 2 3 4 5])
  :put! (fn [ctx]
          (let [body (get-in ctx [:request :body-params])]
            (doseq [id [1 2 3 4 5]]
              (let [event {:id id
                           :state (string/upper-case (:state body))
                           :date (l/local-now)}]
                (switch id (string/upper-case (:state body)))
                (cache-light-event event)
                (publish-light-event event)
                (store-light-event event))))))

(defresource one-light [id]
  :available-media-types ["application/json"]
  :allowed-methods [:get :put]
  :exists? (fn [_]
             (let [light (find-one id)]
               (if (seq light)
                 {:entity light}
                 false)))
  :handle-ok :entity
  :put! (fn [ctx]
          (let [body (get-in ctx [:request :body-params])
                event {:id (read-string id)
                       :state (string/upper-case (:state body))
                       :date (l/local-now)}]
            (switch id (string/upper-case (:state body)))
            (cache-light-event event)
            (publish-light-event event)
            (store-light-event event))))
