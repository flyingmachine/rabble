(ns rabble.resources.post-test
  (:require [rabble.resources.post :as post]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations]
            [rabble.ring-app :as ra])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(def post-options
  {:list {:mapifier post/post}
   :create {:validation validations/topic
            :after-create (fn [ctx record])}
   :show {:mapifier post/post}})

(defn test-app
  []
  (resource-app "/posts"
                post/resource-decisions
                post-options
                shared/default-decisions))

(facts "creation"
  (fact "a valid post with a valid user results in success"
    (app-data test-app :post "/posts" {:content "test" :topic-id (topic-id)} (auth))
    => (contains {"topic-id" (topic-id)}))
  (fact "creating a valid post without a user results in failure"
    (app-req test-app :post "/posts" {:content "test" :topic-id (topic-id)} nil)
    => (contains {:status 401})))
