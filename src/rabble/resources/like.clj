(ns rabble.resources.like
  (:require [datomic.api :as d]
            [rabble.db.transactions.posts :as tx]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.resources.shared :refer :all]
            [rabble.models.permissions :refer :all]
            [rabble.email.sending.notifications :as n]
            [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj]
            [flyingmachine.webutils.utils :refer :all]))

(defn find-like
  [like]
  (dj/one [:like/post (:like/post like)]
          [:like/user (:like/user like)]))

(defn clean-params
  [ctx]
  (c/mapify (merge (params ctx) {:user-id (:id (auth ctx))}) mr/like->txdata))

(defn resource-decisions
  [options defaults app-config]
  (merge-with
   merge defaults
   {:create {:authorized? ctx-logged-in?
             :post! (fn [ctx]
                      (let [like-params (clean-params ctx)]
                        (if-not (find-like like-params)
                          (dj/t [like-params]))))
             :handle-created ""}
    :delete {:authorized? (fn [ctx]
                            (if-let [like (find-like (dissoc (clean-params ctx) :db/id))]
                              {:record (:db/id like)}
                              [false auth-error]))
             :delete! (fn [ctx] @(dj/retract (:record ctx)))}}))
