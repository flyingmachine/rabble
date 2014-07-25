(defproject rabble "0.2.1"
  :description "forum api"
  :url "https://github.com/flyingmachine/rabble"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [org.clojure/core.memoize "0.5.6"]
                 [com.taoensso/timbre "3.0.0-RC4"]
                 [ring "1.3.0"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [crypto-random "1.1.0"]
                 [clavatar "0.2.1"]
                 [environ "0.4.0"]
                 [stencil "0.3.2"]
                 [clj-time "0.6.0"]
                 [compojure "1.1.8"]
                 [liberator "0.11.1"]
                 [com.cemerick/friend "0.1.5"]
                 [me.raynes/cegdown "0.1.0"]
                 [ring-middleware-format "0.3.1"]
                 [com.flyingmachine/webutils "0.1.6"]
                 [flyingmachine/cartographer "0.1.1"]
                 [com.flyingmachine/penny-black-postal "0.1.3"]
                 [com.flyingmachine/datomic-junk "0.1.3"]
                 [com.flyingmachine/liberator-templates "0.1.1"]]

  :plugins [[lein-environ "0.4.0"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[com.datomic/datomic-free "0.9.4360"]
                                  [midje "1.5.0"]
                                  [ring "1.3.0"]
                                  [ring-mock "0.1.4"]
                                  [peridot "0.3.0"]]
                   :env {:com-flyingmachine-penny-black
                         {:template-path "email-templates"
                          :send-email false
                          :from-address "notifications@rabble-forum.com"
                          :from-name "Forum Notifications"}
                         :datomic {:db-uri "datomic:free://localhost:4334/rabble"
                                   :test-uri "datomic:mem://rabble-test"}
                         :rabble {:moderator-names ["flyingmachine"]
                                  :forum-name "Rabble"
                                  :per-page 50}}}
             :test [:dev]})
