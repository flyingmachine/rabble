(ns rabble.resources.topic-test
  (:require [rabble.resources.topic :as topic]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations]
            [rabble.ring-app :as ra])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers))

(setup-db-background)

(def topic-options
  {:list {:mapifier topic/list-topic}
   :create {:validation validations/topic
            :after (fn [ctx param record])}})

(defresources topic/resource-decisions topic-options shared/default-decisions)

(def test-app (test-route (compojure.core/ANY "/topics" [] (collection))))

(fact "query returns topics and pagination info"
  (reload)
  (let [data (data (test-app (jreq :get "/topics")))]
    (first data) => {"page-count" 1 "ent-count" 2 "current-page" 1}
    data => (three-of map?)))

(fact "creating a topic with a valid user results in success"
  (let [response (test-app (jreq :post "/topics" {:content "test"} (auth)))
        data (data response)]
    response => (contains {:status 201})
    data => (contains {"post-count" 1})))

(fact "creating a topic without a user results in failure"
  (test-app (jreq :post "/topics" {:content "test"} nil))
  => (contains {:status 401}))

(fact "creating a topic without content returns errors"
  (test-app (jreq :post "/topics" {} (auth)))
  => (contains {:status 400}))
