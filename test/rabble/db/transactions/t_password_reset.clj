(ns rabble.db.transactions.t-password-reset
  (:require [rabble.test.db-helpers :as tdb]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.transactions.password-reset :as pr]
            [rabble.db.validations :as validations]
            [flyingmachine.cartographer.core :as c]
            [flyingmachine.webutils.validation :refer (validate)]
            [cemerick.friend.credentials :as friend]
            [clj-time.core :as time])
  (:use midje.sweet
        rabble.test.controller-helpers))

(setup-db-background)
(defn user
  ([]
     (user "flyingmachine"))
  ([username]
     (dj/one [:user/username username])))

(fact "generating a token and consuming it will change a password"
  (pr/create-token (user))
  (pr/consume-token (user) "newpass")
  (friend/bcrypt-verify "newpass" (:user/password (user)))
  => true)

(fact "after generating a token, the token is valid"
  (pr/create-token (user))
  (validate {:token (:user/password-reset-token (user))}
            validations/password-reset-token)
  => empty?)

(facts "invalid tokens"
  (fact "a token is invalid if it's not the same as the generated token"
    (pr/create-token (user))
    (validate {:token "a"}
              validations/password-reset-token)
    => {:token ["Your password reset token is invalid. Please go through the password reset process again."]})

  (fact "a token is invalid if it's more than 24 hours old"
    (pr/create-token (user))
    (dj/t [{:db/id (:db/id (user))
           :user/password-reset-token-generated-at (.toDate (time/ago (time/hours 25)))}])
    (validate {:token (:user/password-reset-token (user))}
              validations/password-reset-token)
    => {:token ["Your password reset token is invalid. Please go through the password reset process again."]}))
