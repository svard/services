(ns services.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [services.temperature :as t]))

(defroutes temperature-routes
  (GET "/" [] t/temperature)
  (GET "/current" [] t/current))

(defroutes api-routes
  (GET "/" [] "Hello World")
  (context "/api/temperature" [] temperature-routes)
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/api api-routes))
