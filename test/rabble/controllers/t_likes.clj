(ns rabble.controllers.t-likes
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.controllers.likes :as likes])
  (:use midje.sweet
        rabble.paths
        rabble.test.controller-helpers))

(setup-db-background)

(defn like
  ([]
     (like (:id (auth))))
  ([userid]
     (dj/one [:like/user userid])))

(fact "creating a like results in success"
  (let [response (res :post (str "/likes/" (post-id)) nil (auth "flyingmachine"))]
    (:db/id (:like/user (like))) => (:id (auth))
    response => (contains {:status 201})))

(fact "deleting a like as the creator results in success"
  (res :post (str "/likes/" (post-id)) nil (auth "flyingmachine"))
  (let [response (res :delete (str "/likes/" (post-id)) nil (auth "flyingmachine"))]
    (like) => nil
    response => (contains {:status 204})))
