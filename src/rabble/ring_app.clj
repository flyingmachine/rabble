(ns rabble.ring-app
  (:use clojure.stacktrace
        [ring.adapter.jetty :only (run-jetty)]
        ring.middleware.params
        ring.middleware.keyword-params
        ring.middleware.nested-params
        ring.middleware.session
        ring.middleware.format
        [compojure.core :as compojure]
        [rabble.middleware.routes :only (rabble-routes auth-routes)]
        [rabble.middleware.auth :only (auth)]
        [rabble.middleware.db-session-store :only (db-session-store)]
        [rabble.config :refer (config)]))

(defn wrap-exception [f]
  (fn [request]
    (try (f request)
         (catch Exception e
           (do
             (.printStackTrace e)
             {:status 500
              :body "Exception caught"})))))

(defn debug [f]
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

(def app (wrap (auth (compojure/routes auth-routes rabble-routes))))

(defn start
  "Start the jetty server"
  []
  (run-jetty #'app {:port (or (config :port) 8080) :join? false}))
