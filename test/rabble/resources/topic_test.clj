(ns rabble.resources.topic-test
  (:require [rabble.resources.topic :as topic]
            [rabble.resources.generate :as g])
  (:use midje.sweet
        rabble.paths
        rabble.test.controller-helpers))

(setup-db-background)

(def resources (g/resources :topic
                            topic/create-resource-configs
                            {:topic {:list {:mapifier topic/list-topic}}}))

(facts "query returns topics and pagination info"
  (reload)
  (let [data (data ((:collection resources) (req :get "/")))]
    (first data) => {"page-count" 1 "ent-count" 2 "current-page" 1}
    data => (three-of map?)))
