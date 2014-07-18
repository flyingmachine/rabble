(ns rabble.resources.like-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.watch :as watch]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations]
            [rabble.ring-app :as ra])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(defn watch
  ([]
     (watch (:id (auth))))
  ([userid]
     (dj/one [:watch/user userid])))

(defn test-app
  []
  (resource-app "/watches" watch/resource-decisions))

(fact "creating a watch results in success"
  (let [auth (auth "joebob")
        response (app-req test-app :post "/watches" {:topic-id (topic-id) :user-id (:id auth)} auth)]
    (:db/id (watch (:id auth))) =not=> nil?
    response => (contains {:status 201})))

(fact "deleting a watch as the creator results in success"
  (let [response (app-req test-app :delete (str "/watches/" (:db/id (watch))) nil (auth "flyingmachine"))]
    (watch) => nil
    response => (contains {:status 204})))
