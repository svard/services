(ns services.resources.light
  (:require [liberator.core :refer [defresource]]))

(defresource lights [req]
  :available-media-types ["text/plain"]
  :handle-ok "Hello lights")

(defresource one-light [id]
  :available-media-types ["text/plain"]
  :handle-ok (str "Hello light " id))
