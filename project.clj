(defproject services "0.1.2"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.7.0-alpha1"]
                 [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
                 [compojure "1.1.8"]
                 [ring/ring-core "1.3.0"]
                 [ring-middleware-format "0.4.0"]
                 [liberator "0.12.0"]
                 [org.clojure/data.json "0.2.4"]
                 [com.novemberain/langohr "3.0.0-rc2"]
                 [com.novemberain/monger "2.0.0"]
                 [com.taoensso/timbre "3.2.1"]
                 [clj-time "0.7.0"]]
  :plugins [[lein-ring "0.8.11"]
            [cider/cider-nrepl "0.7.0"]]
  :ring {:handler services.handler/app
         :init services.handler/init
         :destroy services.handler/destroy}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
