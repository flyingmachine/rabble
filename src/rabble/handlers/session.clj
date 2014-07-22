(ns rabble.handlers.session
  (:require [cemerick.friend :as friend]))

(defn create!
  [request]
  (if-let [auth (friend/current-authentication request)]
    {:body (select-keys auth [:username :id :about :display-name])}))
