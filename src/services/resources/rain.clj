(ns services.resources.rain
  (:require [liberator.core :refer [defresource]]
            [clojure.data.json :as json]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defn handle-rain-measure [ch metadata ^bytes payload]
  (let [measure (json/read-str (String. payload "UTF-8") :key-fn keyword)]
    (info measure)))

(defresource rain [{:keys [params] :as req}]
  :available-media-types ["text/plain"]
  :handle-ok "Hello rain")
