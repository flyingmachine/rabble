(ns rabble.resources.user-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.user :as user]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(def user-options
  {:list {:user-mapifier user/user}
   :show {:user-mapifier user/user
          :post-mapifier user/post}})

(defn test-app
  []
  (resource-app "/users" user/resource-decisions user-options))

(fact "You can view all users"
  (count (app-data test-app :get "/users"))
  => 2)

(fact "You can view a user"
  (app-data test-app :get (user-path (:id (auth))))
  => (contains {"email" "daniel@flyingmachinestudios.com"}))

(facts "Users can update stuff"
  (fact "A user can update his own profile"
    (app-data test-app :put (user-path (:id (auth))) {:about "new about" :email "daniel@flyingmachinestudios.com"} (auth))
    => (contains {"about" "new about"}))

  (fact "Your email address must look kind of like an email address"
    (app-req test-app :put (user-path (:id (auth))) {:about "new about" :email "daniel"} (auth))
    => (contains {:status 400})))
