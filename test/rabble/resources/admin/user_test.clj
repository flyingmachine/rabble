(ns rabble.resources.admin.user-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.admin.user :as user]
            [rabble.db.validations :as validations])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(def user-options
  {:list {:mapifier user/user}})

(defn test-app
  []
  (resource-app "/admin/users" user/resource-decisions user-options))

(fact "an admin gets a list of users"
  (count (app-data test-app :get "/admin/users" nil (auth)))
  => 2)
