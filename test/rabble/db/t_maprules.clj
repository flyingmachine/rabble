(ns rabble.db.t-maprules
  (:require [rabble.test.db-helpers :as tdb]
            [rabble.db.maprules :as mr]
            [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj])
  (:use midje.sweet
        rabble.test.controller-helpers))

(setup-db-background)

(fact "topic serializer correctly serializes attributes"
  (c/mapify (dj/one [:topic/title "First topic"]) mr/ent->topic)
  => (contains {:id number?
                :title "First topic"
                :post-count 2
                :author-id number?}))

(fact "topic serializer correctly serializes relationships"
  (:first-post (c/mapify (dj/one [:topic/title "First topic"]) mr/ent->topic {:include :first-post}))
  => (contains {:content "T1 First post content"}))
