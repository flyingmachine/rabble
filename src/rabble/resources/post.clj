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

(defn- search
  [query]
  (map first
       (d/q '[:find ?post
              :in $ ?search
              :where
              [(fulltext $ :post/content ?search) [[?post ?content]]]]
            (dj/db)
            query)))

(defn- all
  []
  (reverse-by :post/created-at (dj/all :post/content)))

(defn- query-ents
  [params]
  (if (empty? (:q params))
    (all)
    (search (:q params))))

(defn resource-config
  [options app-config]
  {:list {:handle-ok (fn [{{params :params} :request}]
                       (mapify-rest
                        (option :list :mapifier)
                        (paginate (query-ents params)
                                  (app-config :per-page)
                                  params)))}
   ;; TODO un-pull params from create-content
   :create {:invalid? (fn [ctx]
                        (validator (params ctx) (:create validations/post)))
            :authorized? (options :create :authorized?)
            :post! (fn [{{params :params auth :auth} :request}]
                     (create-content (options :create :transaction-fn)
                                     params
                                     auth ;; TODO
                                     (options :after-create)))
            :new? true
            :respond-with-entity? true
            :handle-created record-in-ctx}
   :update {:invalid? (fn [ctx]
                        (validator (params ctx) (:delete validations/post)))
            :authorized? (options :update :authorized?)
            :put! (fn [{{params :params} :request}]
                    ())}
   :delete {}})
