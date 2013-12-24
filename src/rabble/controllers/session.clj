(ns rabble.controllers.session
  (:require [cemerick.friend :as friend]))

(defn create!
  [params]
  (if-let [auth (friend/current-authentication)]
    {:body (select-keys auth [:username :id :about])}))
