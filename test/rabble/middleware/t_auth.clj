(ns rabble.middleware.t-auth
  (:use midje.sweet
        ring.mock.request)
  (:require [rabble.db.test :as tdb]
            [rabble.controllers.test-helpers :refer :all]
            [rabble.middleware.auth :as auth]))

(setup-db-background)

(tdb/with-test-db
  (fact "a successful form login returns a success status code and user info"
    (app (request :post "/login" {:username "flyingmachine"
                                  :password "password"}))
    => (contains {:status 200}))

  (fact "a failed form login returns a 401"
    (app (request :post "/login" {:username "flyingmachine"
                                  :password "badpass"}))
    => (contains {:status 401})))


