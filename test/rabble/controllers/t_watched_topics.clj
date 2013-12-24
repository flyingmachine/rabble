(ns rabble.controllers.t-watched-topics
  (:require [rabble.controllers.watched-topics :as watched-topics])
  (:use midje.sweet
        rabble.paths
        rabble.controllers.test-helpers))

(setup-db-background)

(fact "query returns all watched topics"
  (response-data :get "/watched-topics" {} (auth))
  => (one-of map?))
