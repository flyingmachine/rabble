(ns rabble.test.db-helpers
  (:require [datomic.api :as d]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.tasks :as db-tasks])
  (:use rabble.config)
  (:import java.io.File))

(def test-db-uri (dj/config :test-uri))

(defmacro with-test-db
  [& body]
  `(with-redefs [dj/*db-uri* test-db-uri]
     ~@body))
