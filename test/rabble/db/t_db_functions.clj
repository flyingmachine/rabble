(ns rabble.db.t-db-functions
  (:require [rabble.test.db-helpers :as tdb]
            [rabble.db.maprules :as mr]
            [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj])
  (:use midje.sweet
        rabble.test.controller-helpers))

(setup-db-background)

(defn watch
  []
  (dj/one [:watch/topic]))

(fact "increment-register"
  (dj/t [[:increment-watch-count (-> (watch) :watch/topic :db/id) 1]])
  (:watch/unread-count (watch))
  => 1)
