(ns rabble.middleware.logging
  (:require [taoensso.timbre :as timbre]
            [clojure.string :as s]))
(timbre/refer-timbre)

(def j (partial s/join "\n"))
(def ignore-namespaces
  ["clojure" "compojure" "org.eclipse" "liberator" "ring.middleware"])

(defn format-request
  [request]
  (j (map #(str (first %) ": " (second %))
          (select-keys request [:params :request-method :uri]))))

(defn stacktrace-filter
  [ignores line]
  (not (re-find (re-pattern (str "^(" (s/join "|" ignores) ")")) line)))

(defn wrap-exception [f]
  (fn [request]
    (try (f request)
         (catch Exception e
           (do
             (println (format-request request))
             (->> (.getStackTrace e)
                  (map str)
                  (filter (partial stacktrace-filter ignore-namespaces))
                  j
                  error)
             {:status 500
              :body "Exception caught"})))))
