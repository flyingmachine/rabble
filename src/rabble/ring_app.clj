(ns rabble.ring-app
  (:use clojure.stacktrace
        [ring.adapter.jetty :only (run-jetty)]
        ring.middleware.params
        ring.middleware.keyword-params
        ring.middleware.nested-params
        ring.middleware.session
        ring.middleware.format
        [rabble.middleware.routes :only (rabble-routes)]
        [rabble.middleware.db-session-store :only (db-session-store)]))


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
      (wrap-session {:cookie-name "rabble-session" :store (db-session-store {})})
      (wrap-restful-format :formats [:json-kw])
      wrap-exception
      wrap-keyword-params
      wrap-nested-params
      wrap-params))
