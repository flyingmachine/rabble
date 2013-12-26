(ns rabble.controllers.t-watches
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.controllers.watches :as watches])
  (:use midje.sweet
        rabble.paths
        rabble.test.controller-helpers))

(setup-db-background)

(defn watch
  ([]
     (watch (:id (auth))))
  ([userid]
     (dj/one [:watch/user userid])))

(fact "creating a watch results in success"
  (let [auth (auth "joebob")
        response (res :post "/watches" {:topic-id (topic-id) :user-id (:id auth)} auth)]
    (:db/id (watch (:id auth))) =not=> nil?
    response => (contains {:status 201})))

(fact "deleting a watch as the creator results in success"
  (let [response (res :delete (str "/watches/" (:db/id (watch))) nil (auth "flyingmachine"))]
    (watch) => nil
    response => (contains {:status 204})))
