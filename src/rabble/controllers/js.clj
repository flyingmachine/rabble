(ns rabble.controllers.js
  (:require [cemerick.friend :as friend]
            [rabble.config :refer (config)]
            [clojure.data.json :as json]))

(defn load-session
  [params auth]
  (let [session-js
        (fn [value]
          {:body (str "angular.module('"
                      (config :angular-module-name)
                      "').value('loadedSession', " value ")")
           :headers {"content-type" "application/javascript"}})]
    (if auth
      (session-js (json/write-str (select-keys auth [:username :id :display-name])))
      (session-js "null"))))
