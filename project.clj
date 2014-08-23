(defproject services "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.1.8"]
                 [ring/ring-core "1.3.0"]
                 [liberator "0.12.0"]
                 [com.novemberain/langohr "3.0.0-rc2"]
                 [com.novemberain/monger "2.0.0"]]
  :plugins [[lein-ring "0.8.11"]
            [cider/cider-nrepl "0.7.0"]]
  :ring {:handler services.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
