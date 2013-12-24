(ns rabble.db.test
  (:require [datomic.api :as d]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.manage :as manage])
  (:use rabble.config)
  (:import java.io.File))

(def test-db-uri (dj/config :test-uri))

(defmacro with-test-db
  [& body]
  `(binding [dj/*db-uri* test-db-uri]
     ~@body))

(defn initialize
  []
  (doall (manage/reload)))
