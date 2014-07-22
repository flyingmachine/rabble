(ns rabble.ring-app
  (:use clojure.stacktrace
        [ring.adapter.jetty :only (run-jetty)]
        ring.middleware.params
        ring.middleware.keyword-params
        ring.middleware.nested-params
        ring.middleware.session
        ring.middleware.format
        ring.middleware.anti-forgery
        [compojure.core :as compojure]
        [rabble.middleware.default-routes :only (app-routes)]
        [rabble.middleware.auth :only (auth)]
        [rabble.middleware.logging :only (wrap-exception)]
        [rabble.middleware.db-session-store :only (db-session-store)]
        [rabble.config :only (config)]
        [flyingmachine.webutils.utils :only (defnpd)]))

(defn debug-middleware
  [f]
  (fn [{:keys [uri request-method params session] :as request}]
    (println "DEBUG:" request)
    (f request)))

(defn wrap
  "All the default middlewares needed to make rabble work"
  [to-wrap]
  (-> to-wrap
      wrap-anti-forgery
      (wrap-session {:cookie-name (or (config :session-name) "rabble-session")
                     :store (db-session-store {})})
      (wrap-restful-format :formats [:json-kw])
      wrap-exception
      wrap-keyword-params
      wrap-nested-params
      wrap-params))

(def app (wrap app-routes))

(defn start
  "Start the jetty server"
  []
  (run-jetty #'app {:port (or (config :port) 8080) :join? false}))
