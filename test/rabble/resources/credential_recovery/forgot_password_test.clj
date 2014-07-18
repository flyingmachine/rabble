(ns rabble.resources.credential-recovery.forgot-password-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.credential-recovery.forgot-password :as forgot-password]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations])
  (:use midje.sweet
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(defn test-app
  []
  (resource-app "/credential-recovery/forgot-password"
                forgot-password/resource-decisions
                {}
                shared/default-decisions
                ":token"))

(fact "when given a valid username, creates token"
  (app-req test-app :post "/credential-recovery/forgot-password" {:username (:username (auth))})
  (dj/ent-count :user/password-reset-token)
  => 1)

(facts "forgot password workflow"
  (app-req test-app :post "/credential-recovery/forgot-password" {:username (:username (auth))})
  (let [token (:user/password-reset-token (dj/one :user/password-reset-token))
        path (str "/credential-recovery/forgot-password/" token)]
    (fact "returns ok when token exists"
      (app-req test-app :get path)
      => (contains {:status 200}))
    (fact "returns 400 when token doesn't exist"
      (app-req test-app :get "/credential-recovery/forgot-password/xyz")
      => (contains {:status 400}))
    (fact "consumes token with update"
      (app-req test-app :put path {:new-password "test1234" :new-password-confirmation "test1234"})
      (dj/ent-count :user/password-reset-token)
      => 0)))
