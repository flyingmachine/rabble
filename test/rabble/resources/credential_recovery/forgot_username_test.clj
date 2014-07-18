(ns rabble.resources.credential-recovery.forgot-username-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.credential-recovery.forgot-username :as forgot-username])
  (:use midje.sweet
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(defn email
  []
  (:user/email (dj/ent (:id (auth)))))

(defn test-app
  []
  (resource-app "/credential-recovery/forgot-username" forgot-username/resource-decisions))

(fact "when given an existing email, returns ok"
  (app-req test-app :post "/credential-recovery/forgot-username" {:email (email)})
  => (contains {:status 201}))

(fact "when give a nonexistant email, returns error"
  (app-req test-app :post "/credential-recovery/forgot-username" {:email "nobody@nobody.com"})
  => (contains {:status 404}))

(fact "when given an invalid input, returns error"
  (app-req test-app :post "/credential-recovery/forgot-username" {:email ""})
  => (contains {:status 400}))
