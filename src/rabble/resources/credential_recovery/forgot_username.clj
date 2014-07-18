(ns rabble.resources.credential-recovery.forgot-username
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.validations :as validations]
            [rabble.email.sending.senders :as email]
            [rabble.resources.shared :refer :all]
            [flyingmachine.webutils.utils :refer :all]))

(defn resource-decisions
  [options defaults app-config]
  (merge-decision-defaults
   {:create {:malformed? (validator validations/forgot-username)
             :exists? (fn [ctx]
                        (add-record-to-ctx (first (dj/all [:user/email (:email (params ctx))]))))
             :can-post-to-missing? false
             :handle-not-found (fn [_] {:errors {:email ["That email address doesn't exist"]}})
             :post! (fn [ctx] (future (email/send-forgot-username (:record ctx))))
             :handle-created {}}}
   defaults))

