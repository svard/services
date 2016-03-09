(ns services.message-bus
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [langohr.consumers :as lc]
            [langohr.basic :as lb]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(def rabbit (atom {}))

(defn connect-and-subscribe! [host exchange routing-key f]
  (let [conn (rmq/connect {:host host})
        chan (lch/open conn)
        q (.getQueue (lq/declare chan
                                 (str "services-queue@" (.. java.net.InetAddress getLocalHost getHostName))
                                 {:exclusive false :auto-delete true}))]
    (swap! rabbit assoc :conn conn :chan chan)
    (lq/bind chan q exchange {:routing-key routing-key})
    (lc/subscribe chan q f {:auto-ack true})))

(defn publish [exchange routing-key msg]
  (lb/publish (:chan @rabbit) exchange routing-key msg))

(defn close! []
  (info "Closing rabbit connection")
  (rmq/close (:chan @rabbit))
  (rmq/close (:conn @rabbit))
  (reset! rabbit {}))
