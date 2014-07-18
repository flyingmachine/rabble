(ns rabble.resources.password
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.resources.shared :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [flyingmachine.webutils.utils :refer :all]
            [flyingmachine.cartographer.core :as c]
            [liberator.core :refer (defresource)]))

(def authuser (mapifier mr/ent->userauth))

(defn- password-params
  [ctx]
  (let [user (authuser (ctx-id ctx))]
    {:new-password (select-keys params [:new-password :new-password-confirmation])
     :current-password (merge (select-keys (params ctx) [:current-password])
                              (select-keys user [:password]))}))

(defresource change-password! [params auth]
  :allowed-methods [:put :post]
  :available-media-types ["application/json"]
  :malformed? (fn [ctx]
                ((validator validations/change-password)
                 (assoc-in ctx [:request :params] (password-params params))))
  :handle-malformed errors-in-ctx
  :authorized? current-user-id?
  :post! (fn [ctx] (dj/t [(c/mapify (params ctx) mr/change-password->txdata)])))
