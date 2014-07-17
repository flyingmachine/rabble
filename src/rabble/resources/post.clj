(ns rabble.resources.post
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [rabble.db.transactions.posts :as tx]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.resources.shared :refer :all]
            [rabble.models.permissions :refer :all]
            [rabble.email.sending.notifications :as n]
            [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj]
            [flyingmachine.webutils.utils :refer :all]))

(def post (mapifier mr/ent->post
                    {:include (merge {:topic {:only [:id :title]}}
                                     author-inclusion-options)}))

(defn search
  [query]
  (map first
       (d/q '[:find ?post
              :in $ ?search
              :where
              [(fulltext $ :post/content ?search) [[?post ?content]]]]
            (dj/db)
            query)))

(defn all
  []
  (reverse-by :post/created-at (dj/all :post/content)))

(defn query-ents
  [params]
  (if (empty? (:q params))
    (all)
    (search (:q params))))

(defn resource-decisions
  [options defaults app-config]
  (merge-decision-defaults
   {:list {:handle-ok (fn [{{params :params} :request}]
                        (mapify-rest
                         (-> options :list :mapifier)
                         (paginate (query-ents params) (app-config :per-page) params)))}
    
    :create {:malformed? (validator (-> options :create :validation))
             :authorized? ctx-logged-in?
             :post! (create-content
                     tx/create-post
                     (-> options :list :mapifier)
                     (-> options :create :after-create))}
    :update {:malformed? (validator (-> options :update :validation))
             :authorized? (can-update-record? (-> options :show :mapifier))
             :put! (update-record tx/update-post)
             :handle-ok (mapify-with (-> options :show :mapifier))}
    :delete {:authorized? (can-delete-record? (-> options :show :mapifier))
             :delete! delete-record-in-ctx}}
   defaults))

(def default-options
  {:list {:mapifier post}
   :create {:validation (:create validations/post)
            :after-create (fn [ctx _]
                            (future (n/notify-users-of-post (params ctx))))}
   :show {:mapifier post}})
