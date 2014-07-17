(ns rabble.resources.watched-topic-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.watched-topic :as watched-topic]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(defn test-app
  []
  (resource-app "/watched-topics"
                watched-topic/resource-decisions
                {:list {:mapifier watched-topic/topic}}
                shared/default-decisions))

(fact "query returns all watched topics"
  (app-data test-app :get "/watched-topics" {} (auth))
  => (two-of map?))
