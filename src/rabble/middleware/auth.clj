(ns rabble.middleware.auth
  (:require [com.flyingmachine.datomic-junk :as dj]
            [datomic.api :as d]
            [rabble.db.validations :as validations]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.db.transactions.users :as tx]
            [rabble.controllers.shared :refer :all]
            [flyingmachine.webutils.validation :refer :all]
            [cemerick.friend :as friend]
            (cemerick.friend [workflows :as workflows]
                             [credentials :as creds])))

(def authuser (mapifier mr/ent->userauth))
(def user (mapifier mr/ent->user))

(defn find-user
  [username]
  (if-let [user-ent (-> (d/q '[:find ?e :in $ ?username 
                               :where
                               [?e :user/username ?u] 
                               [(clojure.string/lower-case ?u) ?lower-username]
                               [(= ?lower-username ?username)]], 
                             (dj/db)
                             (clojure.string/lower-case username))
                        ffirst
                        dj/ent)]
    (authuser user-ent)))

(defn session-store-authorize
  [{:keys [uri request-method params session]}]
  (when (nil? (:cemerick.friend/identity session))
    (if-let [username (get-in session [:cemerick.friend/identity :current])]
      (workflows/make-auth (select-keys (find-user username) [:id :username])))))

(defn attempt-registration
  "Create a new user and log them in"
  [req]
  (let [{:keys [uri request-method params session rabble]} req]
    (when (and (= uri "/users")
               (= request-method :post))
      (if-valid
       params (:create validations/user) errors
       (workflows/make-auth
        (mapify-tx-result (tx/create-user params) user)
        {:cemerick.friend/redirect-on-auth? false})
       (invalid errors)))))

(defn auth
  [ring-app]
  (friend/authenticate
   ring-app
   {:credential-fn (partial creds/bcrypt-credential-fn find-user)
    :workflows [(workflows/interactive-form
                 :redirect-on-auth? false
                 :login-failure-handler
                 (fn [req]
                   {:body {:errors {:username ["invalid username or password"]}}
                    :status 401}))
                attempt-registration
                session-store-authorize]
    :redirect-on-auth? false
    :login-uri "/login"
    :unauthorized-redirect-uri "/login"}))
