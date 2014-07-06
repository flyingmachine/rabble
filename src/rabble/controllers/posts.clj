(ns rabble.controllers.posts
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [rabble.db.transactions.posts :as tx]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.controllers.shared :refer :all]
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

(defquery
  :return (fn [ctx]
            (mapify-rest
             post
             (paginate (query-ents params) (config :per-page) params))))

(defupdate!
  :invalid? (validator params (:update validations/post))
  :authorized? (can-update-record? post auth)
  :put! (update-record params tx/update-post)
  :return (mapify-with post))

(defcreate!
  :invalid? (validator params (:create validations/post))
  :authorized? (logged-in? auth)
  :post! (create-content
          tx/create-post params auth post
          (fn [ctx params _]
            (future (n/notify-users-of-post params))))
  :return record-in-ctx)

(defdelete!
  :authorized? (can-delete-record? post auth)
  :delete! delete-record-in-ctx)
