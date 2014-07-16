(ns rabble.resources.topic-test
  (:require [rabble.resources.topic :as topic]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations]
            [rabble.ring-app :as ra])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(def topic-options
  {:list {:mapifier topic/list-topic}
   :create {:validation validations/topic
            :after (fn [ctx param record])}
   :show {:mapifier topic/topic}})

(defn test-app
  []
  (resource-app "/topics"
                topic/resource-decisions
                topic-options
                shared/default-decisions))

(fact "query returns topics and pagination info"
  (let [data (data ((test-app) (jreq :get "/topics")))]
    (first data) => {"page-count" 1 "ent-count" 2 "current-page" 1}
    data => (three-of map?)))

(facts "creating a topic"
  (fact "with a valid user results in success"
    (let [response ((test-app) (jreq :post "/topics" {:content "test"} (auth)))
          data (data response)]
      response => (contains {:status 201})
      data => (contains {"post-count" 1})))

  (fact "without a user results in failure"
    ((test-app) (jreq :post "/topics" {:content "test"} nil))
    => (contains {:status 401}))

  (fact "without content returns errors"
    (let [response ((test-app) (jreq :post "/topics" {} (auth)))
          data (data response)]
      response
      => (contains {:status 400})
      (keys data)
      => ["errors"])))

(facts "showing a topic"
  (fact "works if the topic exists"
    (let [id (topic-id)]
      (data ((test-app) (jreq :get (topic-path id))))
      => (contains {"id" id}))))

(facts "deleting a topic"
  (facts "ownership"
    (fact "deleting a topic as the author results in success"
      ((test-app) (jreq :delete (topic-path (topic-id)) nil (auth "flyingmachine")))
      => (contains {:status 204}))
    (fact "deleting a non-existent topic results in nonexistent code"
      ((test-app) (jreq :delete (topic-path "101010") nil (auth "flyingmachine")))
      => (contains {:status 404}))
    (fact "deleting a topic as not the author results in failure"
      ((test-app) (jreq :delete (topic-path (topic-id)) nil (auth "joebob")))
      => (contains {:status 401}))))
