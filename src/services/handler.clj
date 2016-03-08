(ns services.handler
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [services.resources.temperature :as t]
            [services.resources.light :as l]
            [services.resources.rain :as r]
            [services.message-bus :as mb]
            [taoensso.timbre :as timbre]
            [ring.middleware.format :as format]
            [ring.middleware.jsonp :as jsonp]))

(timbre/refer-timbre)

(defroutes temperature-routes
  (GET "/" [] t/temperature)
  (POST "/" [] t/temperature)
  (GET "/current" [] t/current)
  (GET "/stats" [] t/stats))

(defroutes light-routes
  (GET "/" [] l/lights)
  (GET "/:id" [id] (partial l/one-light id))
  (PUT "/" [] l/lights)
  (PUT "/:id" [id] (partial l/one-light id)))

(defroutes rain-routes
  (GET "/" [] r/rain))

(defroutes api-routes
  (GET "/" [] "Hello World")
  (context "/api/temperature" [] temperature-routes)
  (context "/api/light" [] light-routes)
  (context "/api/rain" [] rain-routes)
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init []
  (timbre/set-level! :debug)
  (info "Starting services")
  (mb/connect-and-subscribe! "192.168.0.108" "automation" "rain" r/handle-rain-measure))

(defn destroy []
  (info "Shutting down services...")
  (mb/close!))

(def app
  (-> api-routes
      (jsonp/wrap-json-with-padding)
      (handler/api)
      (format/wrap-restful-format :formats [:json-kw])))
