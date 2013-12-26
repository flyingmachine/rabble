(ns rabble.middleware.t-auth
  (:use midje.sweet
        ring.mock.request)
  (:require [rabble.test.controller-helpers :refer :all]
            [rabble.middleware.auth :as auth]))

(setup-db-background)

(fact "a successful form login returns a success status code and user info"
  (res :post "/login" {:username "flyingmachine" :password "password"})
  => (contains {:status 200}))

(fact "a failed form login returns a 401"
  (res :post "/login" {:username "flyingmachine" :password "badpass"})
  => (contains {:status 401}))
