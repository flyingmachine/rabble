(ns rabble.controllers.js
  (:require [cemerick.friend :as friend]
            [rabble.config :refer (config)]
            [clojure.data.json :as json]
            [ring.middleware.anti-forgery :as af]))

(defn session-js
  [value]
  {:body (str "angular.module('"
                      (config :angular-module-name)
                      "').value('loadedSession', " value ")")
           :headers {"content-type" "application/javascript"}})

(defn load-session
  [params auth]
  (let [af-token {:anti-forgery-token ring.middleware.anti-forgery/*anti-forgery-token*}
        session (json/write-str (if auth
                                  (merge (select-keys auth [:username :id :display-name])
                                         af-token)
                                  af-token))]
    (session-js session)))
