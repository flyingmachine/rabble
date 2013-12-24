(ns rabble.controllers.users
  (:require [rabble.db.validations :as validations]
            [com.flyingmachine.datomic-junk :as dj]
            [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [rabble.db.transactions.users :as tx]
            [flyingmachine.cartographer.core :as c]
            [cemerick.friend :as friend]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defshow defupdate!)]
            cemerick.friend.workflows)
  (:use [flyingmachine.webutils.validation :only (if-valid)]
        [liberator.core :only [defresource]]
        rabble.models.permissions
        rabble.db.mapification
        rabble.controllers.shared
        flyingmachine.webutils.utils))

(defmapifier record mr/ent->user)
(defmapifier authrecord mr/ent->userauth)

(defn attempt-registration
  [req]
  (let [{:keys [uri request-method params session]} req]
    (when (and (= uri "/users")
               (= request-method :post))
      (if-valid
       params (:create validations/user) errors
       (cemerick.friend.workflows/make-auth
        (mapify-tx-result (tx/create-user params) record)
        {:cemerick.friend/redirect-on-auth? false})
       (invalid errors)))))

(defn registration-success-response
  [params auth]
  "If the request gets this far, it means that user registration was successful."
  (if auth {:body auth}))

(defn show-opts
  [params]
  (if (:include-posts params)
    {:include
     {:posts {:include
              {:topic {:only [:title :id]}}}}}
    {}))

(defshow
  [params]
  :exists? (exists? (record (id) (show-opts params)))
  :return record-in-ctx)

(defn update!*
  [params]
  (dj/t [[:db/retract (str->int (:id params)) :user/preferences tx/preferences] ; remove all existing prefs
         (remove-nils-from-map
          (c/mapify params
                    mr/user->txdata
                    {:exclude [:user/username :user/password]}))]))

(defupdate!
  :invalid? (validator params (validations/email-update auth))
  :authorized? (current-user-id? (id) auth)
  :exists? (fn [_] (dj/ent (id)))
  :put! (fn [_] (update!* params))
  :return (fn [_] (record (id))))

(defn- password-params
  [params]
  (let [user (authrecord (id))]
    {:new-password (select-keys params [:new-password :new-password-confirmation])
     :current-password (merge (select-keys params [:current-password]) {:password (:password user)})}))

(defresource change-password! [params auth]
  :allowed-methods [:put :post]
  :available-media-types ["application/json"]

  :malformed? (validator (password-params params) validations/change-password)
  :handle-malformed errors-in-ctx
  
  :authorized? (fn [_] (current-user-id? (id) auth))
  
  :post! (fn [_] (dj/t [(c/mapify params mr/change-password->txdata)])))
