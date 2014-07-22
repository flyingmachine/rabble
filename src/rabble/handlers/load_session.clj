(ns rabble.handlers.load-session
  (:require [cemerick.friend :as friend]
            [rabble.config :refer (config)]
            [clojure.data.json :as json]
            [ring.middleware.anti-forgery :as af]))

(def session-user-keys (or (config :session-user-keys) [:username :id :display-name]))

(defn session-js
  [value]
  {:body (str "angular.module('"
              (config :angular-module-name)
              "').value('loadedSession', " value ")")
   :headers {"content-type" "application/javascript"}})

(defn load-session
  [auth]
  (let [af-token {:anti-forgery-token af/*anti-forgery-token*}
        session (json/write-str (if auth
                                  (merge (select-keys auth session-user-keys)
                                         af-token)
                                  af-token))]
    (session-js session)))
