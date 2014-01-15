(ns rabble.ring-app
  (:require rabble.lib.dispatcher)
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
        [rabble.middleware.dispatcher :only (add-rabble-dispatcher)]
        [rabble.middleware.db-session-store :only (db-session-store)]
        [rabble.config :refer (config)]
        [flyingmachine.webutils.utils :only (defnpd)])
  (:import [rabble.lib.dispatcher RabbleDispatcher]))

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

(defnpd site
  [[middlewares []] [routes []]]
  (let [router (apply compojure/routes (conj (vec routes) rabble-routes))
        stack (into [router] (conj (vec (reverse middlewares)) wrap))]
    (reduce #(%2 %1) stack)))

(def app (site [auth (add-rabble-dispatcher (RabbleDispatcher.))] [auth-routes]))

(defn start
  "Start the jetty server"
  []
  (run-jetty #'app {:port (or (config :port) 8080) :join? false}))
