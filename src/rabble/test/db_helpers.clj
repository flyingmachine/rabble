(ns rabble.test.db-helpers
  (:require [datomic.api :as d]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.tasks :as db-tasks]
            [flyingmachine.webutils.utils :refer :all]
            [rabble.config :refer :all])
  (:use midje.sweet)
  (:import java.io.File))

(def test-db-uri (dj/config :test-uri))

(defn reload
  []
  (db-tasks/reload)
  (dj/t (read-resource "fixtures/seeds.edn")))

(defmacro with-test-db
  [& body]
  `(binding [dj/*db-uri* test-db-uri]
     ~@body))

(defmacro setup-db-background
  [& before]
  `(background
    (before :contents (with-test-db
                        (reload)
                        ~@before))
    (around :facts (with-test-db ?form))))

(defn auth
  ([] (auth "flyingmachine"))
  ([username]
     {:id (:db/id (dj/one [:user/username username]))
      :username username}))
