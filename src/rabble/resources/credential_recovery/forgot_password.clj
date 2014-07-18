(ns rabble.resources.credential-recovery.forgot-password
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.validations :as validations]
            [rabble.db.transactions.password-reset :as tx]
            [rabble.email.sending.senders :as email]
            [rabble.resources.shared :refer :all]
            [flyingmachine.webutils.utils :refer :all]))

(defn resource-decisions
  [options defaults app-config]
  (merge-decision-defaults
   {:show {:malformed? (validator validations/password-reset-token)
           :respond-with-entity? false
           :handle-ok {}}
    
    :create {:malformed? (validator validations/forgot-password)
             :exists? (fn [ctx]
                        (add-record-to-ctx
                         (first (dj/all [:user/username (:username (params ctx))]))))
             :can-post-to-missing? false
             :handle-not-found (fn [_] {:errors {:username ["That username isn't in our system"]}})
             :post! (fn [ctx]
                      (let [user (:record ctx)]               
                        (tx/create-token user)
                        (future (email/send-password-reset-token [(dj/ent (:db/id user))])))
                      {})
             :handle-created {}}

    :update {:malformed? (fn [ctx]
                           (let [params (params ctx)]
                             ((validator validations/password-reset)
                              (assoc-in ctx [:request :params :new-password] params))))
             :exists? (fn [ctx]
                        (if-let [user (dj/one [:user/password-reset-token (:token (params ctx))])]
                          {:record user}))
             :put! (fn [ctx] (tx/consume-token (:record ctx) (:new-password (params ctx))))
             :new? false
             :respond-with-entity? false
             :handle-ok {}}}
   defaults))
