(ns rabble.ring-app
  (:require rabble.lib.dispatcher)
  (:use clojure.stacktrace
        [ring.adapter.jetty :only (run-jetty)]
        ring.middleware.params
        ring.middleware.keyword-params
        ring.middleware.nested-params
        ring.middleware.session
        ring.middleware.format
        ring.middleware.anti-forgery
        [compojure.core :as compojure]
        [rabble.middleware.routes :only (rabble-routes auth-routes)]
        [rabble.middleware.auth :only (auth)]
        [rabble.middleware.logging :only (wrap-exception)]
        [rabble.middleware.dispatcher :only (rabble-dispatcher)]
        [rabble.middleware.db-session-store :only (db-session-store)]
        [rabble.config :only (config)]
        [flyingmachine.webutils.utils :only (defnpd)])
  (:import [rabble.lib.dispatcher RabbleDispatcher]))

(defn debug-middleware [f]
  (fn [{:keys [uri request-method params session] :as request}]
    (println params)
    (f request)))

(defn wrap
  [to-wrap]
  (-> to-wrap
      (wrap-session {:cookie-name (or (config :session-name) "rabble-session")
                     :store (db-session-store {})})
      (wrap-restful-format :formats [:json-kw])
      wrap-exception
      wrap-keyword-params
      wrap-nested-params
      wrap-params))

(defnpd site
  [[middlewares []] [routes []]]
  (let [router (apply compojure/routes (concat routes [rabble-routes]))
        stack (into [router] (concat (reverse middlewares) [wrap]))]
    (reduce #(%2 %1) stack)))

(def app (site [wrap-anti-forgery auth] [auth-routes]))

(defn start
  "Start the jetty server"
  []
  (run-jetty #'app {:port (or (config :port) 8080) :join? false}))
