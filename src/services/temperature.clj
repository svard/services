(ns services.temperature
  (:require [liberator.core :refer [defresource]]))

(defresource current [req]
  :available-media-types ["text/plain"]
  :handle-ok "Hello current")

(defresource temperature [{:keys [params] :as req}]
  :available-media-types ["text/plain"]
  :handle-ok (fn [_]
               (str "Hello " (:year params))))

