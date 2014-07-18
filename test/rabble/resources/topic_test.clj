(ns rabble.resources.topic-test
  (:require [rabble.resources.topic :as topic]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(def topic-options
  {:list {:mapifier topic/list-topic}
   :create {:validation validations/topic
            :after-create (fn [ctx record])}
   :show {:mapifier topic/topic}})

(defn test-app
  []
  (resource-app "/topics" topic/resource-decisions topic-options))

(fact "query returns topics and pagination info"
  (let [data (app-data test-app :get "/topics")]
    (first data) => {"page-count" 1 "ent-count" 2 "current-page" 1}
    data => (three-of map?)))

(facts "creating a topic"
  (fact "with a valid user results in success"
    (let [response (app-req test-app :post "/topics" {:content "test"} (auth))
          data (data response)]
      response => (contains {:status 201})
      data => (contains {"post-count" 1})))

  (fact "without a user results in failure"
    (app-req test-app :post "/topics" {:content "test"} nil)
    => (contains {:status 401}))

  (fact "without content returns errors"
    (let [response (app-req test-app :post "/topics" {} (auth))
          data (data response)]
      response
      => (contains {:status 400})
      (keys data)
      => ["errors"])))

(facts "showing a topic"
  (fact "works if the topic exists"
    (let [id (topic-id)]
      (app-data test-app :get (topic-path id))
      => (contains {"id" id}))))

(facts "deleting a topic"
  (facts "ownership"
    (fact "deleting a topic as the author results in success"
      (app-req test-app :delete (topic-path (topic-id)) nil (auth "flyingmachine"))
      => (contains {:status 204}))
    (fact "deleting a non-existent topic results in nonexistent code"
      (app-req test-app :delete (topic-path "101010") nil (auth "flyingmachine"))
      => (contains {:status 404}))
    (fact "deleting a topic as not the author results in failure"
      (app-req test-app :delete (topic-path (topic-id)) nil (auth "joebob"))
      => (contains {:status 401}))))
