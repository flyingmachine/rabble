(ns rabble.middleware.auth-test
  (:require [rabble.test.resource-helpers :refer :all]
            [rabble.test.db-helpers :refer :all]
            [rabble.middleware.default-routes :refer (app-routes)]
            [rabble.middleware.auth :as auth])
  (:use midje.sweet
        ring.mock.request))

(setup-db-background)

(def app (wrap (auth/auth app-routes)))

(fact "a successful form login returns a success status code and user info"
  (app (jreq :post "/login" {:username "flyingmachine" :password "password"}))
  => (contains {:status 200}))

(fact "a failed form login returns a 401"
  (app (jreq :post "/login" {:username "flyingmachine" :password "badpass"}))
  => (contains {:status 401}))
