(ns rabble.controllers.t-topics
  (:require [rabble.controllers.topics :as topics])
  (:use midje.sweet
        rabble.paths
        rabble.test.controller-helpers))

(setup-db-background)

(facts "query returns topics and pagination info"
  (reload)
  (let [data (response-data :get "/topics" {})]
    (first data) => {"page-count" 1 "ent-count" 2 "current-page" 1}
    data => (three-of map?)))

(fact "creating a topic with a valid user results in success"
  (let [response (res :post "/topics" {:content "test"} (auth))
        data (data response)]
    response => (contains {:status 201})
    data => (contains {"post-count" 1})))

(fact "creating a topic without a user results in failure"
  (res :post "/topics" {:content "test"} nil)
  => (contains {:status 401}))

(fact "creating a topic without content returns errors"
  (res :post "/topics" {} (auth))
  => (contains {:status 400}))

;; Showing

(fact "attempting to view an existing topic returns the topic"
  (let [id (topic-id)]
    (response-data :get (topic-path (topic-id)))
    => (contains {"id" id})))

(fact "attempting to view a nonexistent topic returns not found"
  (res :get "/topics/101010")
  => (contains {:status 404}))

(facts "topics can only be deleted by their authors"
  (fact "deleting a topic as the author results in success"
    (res :delete (topic-path (topic-id)) nil (auth "flyingmachine"))
    => (contains {:status 204}))
  (fact "deleting a non-existent topic results in nonexistent code"
    (res :delete (topic-path "101010") nil (auth "flyingmachine"))
    => (contains {:status 404}))
  (fact "deleting a topic as not the author results in failure"
    (res :delete (topic-path (topic-id)) nil (auth "joebob"))
    => (contains {:status 401})))
