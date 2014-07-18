(ns rabble.resources.user
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [rabble.db.transactions.users :as tx]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.resources.shared :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [flyingmachine.webutils.utils :refer :all]))

(def user (mapifier mr/ent->user {:exclude [:preferences]}))
(def post (mapifier mr/ent->post {:include {:topic {:only [:title :id]}}}))
(def user->txdata (mapifier mr/user->txdata {:exclude [:user/username :user/password]}))

(defn posts
  [params author-id mapifier app-config]
  (mapify-rest 
   mapifier
   (paginate (reverse-by :post/created-at (dj/all :post/content [:content/author author-id]))
             (or (app-config :per-page) 20)
             params)))

(defn user-sort
  "Sorts by last name, if display-name contains firstname lastname
  TODO improve"
  [users]
  (sort-by (fn [user]
             (if-let [display-name (:display-name user)]
               (let [split (-> display-name
                               (clojure.string/lower-case)
                               (clojure.string/split #" "))]
                 (->> split
                      (take 2)
                      reverse
                      (clojure.string/join " ")))
               (:username user)))
           users))

(defn resource-decisions
  [options defaults app-config]
  (merge-decision-defaults
   ;; TODO for post maybe use a different mapifier including posts?
   {:list {:handle-ok (fn [ctx]
                        (user-sort (map (-> options :list :user-mapifier)
                                        (dj/all :user/username))))}
    ;; The request will reach here after going through the
    ;; "attempt-registration" auth workflow
    :create {:handle-ok auth}
    :show {:exists? (fn [ctx]
                      (if-let [r ((exists? (-> options :show :user-mapifier)) ctx)]
                        (assoc-in r
                                  [:record :posts]
                                  (posts (params ctx)
                                         (ctx-id ctx)
                                         (-> options :show :post-mapifier)
                                         app-config))))
           :handle-ok record-in-ctx}
    :update {:malformed? (fn [ctx] ((validator (validations/email-update (auth ctx))) ctx))
             :authorized? current-user-id
             :exists? (fn [ctx] (dj/ent (ctx-id ctx)))
             :put! (fn [ctx]
                     (dj/t [[:db/retract (ctx-id ctx) :user/preferences tx/preferences]])
                     (dj/t [(remove-nils-from-map (user->txdata (params ctx)))]))
             :handle-ok (mapify-with (-> options :show :user-mapifier))}}
   defaults))

(def default-options
  {:list {:user-mapifier user}
   :show {:user-mapifier user
          :post-mapifier post}})
