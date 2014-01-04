(ns rabble.test.controller-helpers
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.test.db-helpers :as tdb]
            [rabble.db.tasks :as db-tasks]
            [rabble.ring-app :refer (app)]
            [rabble.middleware.routes :as routes]
            [clojure.data.json :as json]
            [compojure.core :as compojure])
  (:use midje.sweet
        flyingmachine.webutils.utils
        rabble.paths
        [ring.mock.request :only [request header content-type]]))

(defn auth
  ([] (auth "flyingmachine"))
  ([username]
     {:id (:db/id (dj/one [:user/username username]))
      :username username}))

(defn authenticated
  [request auth]
  (if auth
    (merge request {:authentications {:test auth}
                    :current :test})
    request))

(defnpd req
  [method path [params nil] [auth nil]]
  (-> (request method path params)
      (content-type "application/json")
      (authenticated auth)))

(defnpd res
  [method path [params nil] [auth nil]]
  (let [params (json/write-str params)]
       (app (req method path params auth))))

(defn data
  [response]
  (-> response
      :body
      json/read-str))

(defnpd response-data
  [method path [params nil] [auth nil]]
  (data (res method path params auth)))

(defmacro setup-db-background
  [& before]
  `(background
    (before :contents (tdb/with-test-db
                        (db-tasks/reload)
                        (dj/t (read-resource "fixtures/seeds.edn"))
                        ~@before))
    (around :facts (tdb/with-test-db ?form))))

(defn content-id
  ([content-attribute] (content-id content-attribute "flyingmachine"))
  ([content-attribute author-username]
     (:db/id (dj/one content-attribute [:content/author (:id (auth author-username))]))))

(def topic-id (partial content-id :topic/title))
(def post-id (partial content-id :post/content))
