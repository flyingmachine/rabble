(ns rabble.resources.post
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [rabble.db.transactions.posts :as tx]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.resources.shared :refer :all]
            [rabble.models.permissions :refer :all]
            [rabble.config :refer (config)]
            [rabble.email.sending.notifications :as n]
            [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj]
            [flyingmachine.webutils.utils :refer :all]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defquery defupdate! defcreate! defdelete!)]))

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
  (merge-with
   merge defaults
   {:list {:handle-ok (fn [{{params :params} :request}]
                        (mapify-rest
                         (-> options :list :mapifier)
                         (paginate (query-ents params) (app-config :per-page) params)))}
    
    :create {:invalid? (validator (-> options :create :validation))
             :authorized? ctx-logged-in?
             :post! (create-content
                     tx/create-post
                     (-> options :list :mapifier)
                     (-> options :create :after-create))}
    :update {:invalid? (fn [ctx]
                         (validator (params ctx) (:delete validations/post)))
             :authorized? (options :update :authorized?)
             :put! (fn [{{params :params} :request}]
                     ())}
    :delete {}}))

(def default-options
  {:list {:mapifier post}
   :create {:validation (:create validations/post)}
   :show {:mapifier post}})
