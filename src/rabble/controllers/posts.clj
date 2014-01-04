(ns rabble.controllers.posts
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [rabble.db.transactions.posts :as tx]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
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
  [mapifier params]
  (map (comp (partial record mapifier) first)
       (d/q '[:find ?post
              :in $ ?search
              :where
              [(fulltext $ :post/content ?search) [[?post ?content]]]]
            (dj/db)
            (:q params))))

(defn all
  [mapifier]
  (reverse-by :created-at (map (partial record mapifier) (dj/all :post/content))))

(defquery
  :return (fn [ctx]
            (let [m (mapifier ctx)]
              (if (not-empty (:q params))
                (search m params)
                (all m)))))

(defupdate!
  :invalid? (validator params (:update validations/post))
  :authorized? (can-update-record? record auth)
  :put! (update-record params tx/update-post)
  :return (mapify-with record))

(defcreate!
  :invalid? (validator params (:create validations/post))
  :authorized? (logged-in? auth)
  :post! (create-content tx/create-post params auth record)
  :return record-in-ctx)

(defdelete!
  :authorized? (can-delete-record? record auth)
  :delete! delete-record-in-ctx)
