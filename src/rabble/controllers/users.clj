(ns rabble.controllers.users
  (:require [rabble.db.validations :as validations]
            [com.flyingmachine.datomic-junk :as dj]
            [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [rabble.db.transactions.users :as tx]
            [rabble.config :refer (config)]
            [flyingmachine.cartographer.core :as c]
            [cemerick.friend :as friend]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defshow defupdate!)]
            cemerick.friend.workflows)
  (:use [flyingmachine.webutils.validation :only (if-valid)]
        [liberator.core :only (defresource)]
        rabble.models.permissions
        rabble.db.mapification
        rabble.controllers.shared
        flyingmachine.webutils.utils))

(defmapifier record mr/ent->user)
(defmapifier authrecord mr/ent->userauth)
(defmapifier post mr/ent->post {:include {:topic {:only [:title :id]}}})

(defn attempt-registration
  [req]
  (let [{:keys [uri request-method params session rabble]} req]
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

(defn posts
  [params author-id]
  (mapify-rest 
   post
   (paginate (reverse-by :post/created-at (dj/all :post/content [:content/author author-id]))
             (config :per-page)
             params)))

(defshow
  [params]
  :exists? (fn [ctx]
             (if-let [r ((exists? #(record %1 %2)) ctx)]
               (assoc-in r
                         [:record :posts]
                         (posts params (ctx-id ctx)))))
  :return record-in-ctx)

(defn update!*
  [params]
  ; remove all existing prefs
  (dj/t [[:db/retract (str->int (:id params)) :user/preferences tx/preferences]])
  (dj/t [(remove-nils-from-map
          (c/mapify params
                    mr/user->txdata
                    {:exclude [:user/username :user/password]}))]))

(defupdate!
  :invalid? (validator params (validations/email-update auth))
  :authorized? (current-user-id? (id) auth)
  :exists? (fn [_] (dj/ent (id)))
  :put! (fn [_] (update!* params))
  :return (mapify-with record))


;; TODO update with exists?
(defn- password-params
  [params]
  (let [user (authrecord (id))]
    {:new-password (select-keys params [:new-password :new-password-confirmation])
     :current-password (merge (select-keys params [:current-password])
                              (select-keys user [:password]))}))

(defresource change-password! [params auth]
  :allowed-methods [:put :post]
  :available-media-types ["application/json"]

  :malformed? (fn [ctx]
                ((validator (password-params params)
                            validations/change-password)
                 ctx))
  :handle-malformed errors-in-ctx
  
  :authorized? (fn [_] (current-user-id? (id) auth))
  
  :post! (fn [_] (dj/t [(c/mapify params mr/change-password->txdata)])))
