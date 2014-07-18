(ns rabble.resources.like-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.like :as like]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(defn like
  ([]
     (like (:id (auth))))
  ([userid]
     (dj/one [:like/user userid])))

(defn test-app
  []
  (resource-app "/likes" like/resource-decisions))

(fact "creating a like results in success"
  (let [response (app-req test-app :post "/likes" {:post-id (post-id)} (auth "flyingmachine"))]
    (:db/id (:like/user (like))) => (:id (auth))
    response => (contains {:status 201})))

(fact "deleting a like as the creator results in success"
  (app-req test-app :post "/likes" {:post-id (post-id)} (auth "flyingmachine"))
  (let [response (app-req test-app :delete (str "/likes/" (post-id)) nil (auth "flyingmachine"))]
    (like) => nil
    response => (contains {:status 204})))
