(ns services.message-bus
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [langohr.consumers :as lc]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(def conn (atom nil))

(defn connect-and-subscribe! [host exchange routing-key f]
  (reset! conn (rmq/connect {:host host}))
  (let [ch (lch/open @conn)]
    (le/declare ch exchange "direct")
    (let [q (.getQueue (lq/declare ch (str "services-queue@" (.. java.net.InetAddress getLocalHost getHostName)) {:exclusive false :auto-delete true}))]
      (lq/bind ch q exchange {:routing-key routing-key})
      (lc/subscribe ch q f {:auto-ack true}))))

(defn close! []
  (info "Closing rabbit connection")
  (reset! conn (rmq/close @conn)))
