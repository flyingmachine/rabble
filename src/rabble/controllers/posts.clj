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


(defmapifier record
  mr/ent->post
  {:include (merge {:topic {:only [:id :title]}}
                   author-inclusion-options)})

(defn search
  [params]
  (map first
       (d/q '[:find ?post
              :in $ ?search
              :where
              [(fulltext $ :post/content ?search) [[?post ?content]]]]
            (dj/db)
            (:q params))))

(defn all
  []
  (reverse-by :post/created-at (dj/all :post/content)))

(defn query-ents
  [params]
  (if (empty? (:q params))
    (all)
    (search params)))

(defquery
  :return (fn [ctx]
            (mapify-rest
             record
             (paginate (query-ents params) (config :per-page) params))))

(defupdate!
  :invalid? (validator params (:update validations/post))
  :authorized? (can-update-record? record auth)
  :put! (update-record params tx/update-post)
  :return (mapify-with record))

(defcreate!
  :invalid? (validator params (:create validations/post))
  :authorized? (logged-in? auth)
  :post! (create-content
          tx/create-post params auth record
          (fn [ctx params _]
            (future (n/notify-users-of-post params))))
  :return record-in-ctx)

(defdelete!
  :authorized? (can-delete-record? record auth)
  :delete! delete-record-in-ctx)
