(ns rabble.resources.admin.user-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.admin.user :as user])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(defn test-app
  []
  (resource-app "/admin/users" user/resource-decisions user/default-options))

(fact "an admin gets a list of users"
  (count (app-data test-app :get "/admin/users" nil (auth)))
  => 2)
