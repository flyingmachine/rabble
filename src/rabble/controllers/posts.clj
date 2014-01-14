(ns rabble.controllers.posts
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [rabble.db.transactions.posts :as tx]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.config :refer (config)]
            [rabble.email.sending.notifications :as n]
            [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defquery defupdate! defcreate! defdelete!)])
  (:import [rabble.middleware.mapifier RabbleMapifier])
  (:use rabble.controllers.shared
        rabble.models.permissions
        flyingmachine.webutils.utils))

(defprotocol PostsController
  (record [mapifier ent]))

(defmapifier record*
  mr/ent->post
  {:include (merge {:topic {:only [:id :title]}}
                   author-inclusion-options)})

(extend-type RabbleMapifier
  PostsController
  (record [mapifier ent] (record* ent)))

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
  (if (empty (:q params))
    (search params)
    (all)))

(defquery
  :return (fn [ctx]
            (mapify-rest
             (mapifier ctx)
             record
             (paginate (query-ents params) (or (config :per-page) 50) params))))

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
            (future (n/notify-users-of-post (mapifier ctx) params))))
  :return record-in-ctx)

(defdelete!
  :authorized? (can-delete-record? record auth)
  :delete! delete-record-in-ctx)
