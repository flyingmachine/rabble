(defproject rabble "0.1.0-SNAPSHOT"
  :description "forum api"
  :url "https://github.com/flyingmachine/rabble"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [ring "1.2.1"]
                 [org.clojure/core.memoize "0.5.6"]
                 [crypto-random "1.1.0"]
                 [markdown-clj "0.9.25"]
                 [clavatar "0.2.1"]
                 [environ "0.4.0"]
                 [stencil "0.3.2"]
                 [clj-time "0.6.0"]
                 [compojure "1.1.5"]
                 [liberator "0.10.0"]
                 [com.cemerick/friend "0.1.5"]
                 [me.raynes/cegdown "0.1.0"]
                 [ring-middleware-format "0.3.1"]
                 [com.flyingmachine/webutils "0.1.6"]
                 [flyingmachine/cartographer "0.1.1"]
                 [com.flyingmachine/penny-black-postal "0.1.0"]
                 [com.flyingmachine/datomic-junk "0.1.3"]
                 [com.flyingmachine/liberator-templates "0.1.1"]]

  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.9.4360"]
                                  [midje "1.5.0"]
                                  [ring "1.2.1"]
                                  [ring-mock "0.1.4"]]
                   :env {:com-flyingmachine-penny-black
                         {:template-path "email-templates"
                          :send-email false
                          :from-address "notifications@rabble-forum.com"
                          :from-name "Forum Notifications"}
                         :datomic {:db-uri "datomic:free://localhost:4334/rabble"
                                   :test-uri "datomic:mem://rabble-test"}
                         :rabble {:moderator-names ["flyingmachine"]
                                  :forum-name "Rabble"}}}
             :test [:dev]})
