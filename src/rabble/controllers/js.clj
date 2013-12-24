(ns rabble.controllers.js
  (:require [cemerick.friend :as friend]))

(defn load-session
  [params auth]
  (let [session-js
        (fn [value]
          {:body (str "angular.module('rabbleApp').value('loadedSession', " value ")")
           :headers {"content-type" "application/javascript"}})]
    (if auth
      (session-js (str "{username:'" (:username auth) "', id: " (:id auth) "}"))
      (session-js "null"))))
