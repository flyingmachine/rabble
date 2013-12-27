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
  (:use rabble.controllers.shared
        rabble.models.permissions
        flyingmachine.webutils.utils))

(defmapifier record
  mr/ent->post
  {:include (merge {:topic {:only [:id :title]}}
                   author-inclusion-options)})

(defn search
  [params]
  (map (comp record first)
       (d/q '[:find ?post
              :in $ ?search
              :where
              [(fulltext $ :post/content ?search) [[?post ?content]]]]
            (dj/db)
            (:q params))))

(defn all
  []
  (reverse-by :created-at
              (map record
                   (dj/all :post/content))))

(defquery [params]
  :return (fn [_]
            (if (not-empty (:q params))
              (search params)
              (all))))

(defupdate!
  :invalid? (validator params (:update validations/post))
  :authorized? (can-update-record? (record (id)) auth)
  :put! (update-record params tx/update-post)
  :return (fn [_] (record (id))))

(defcreate!
  :invalid? (validator params (:create validations/post))
  :authorized? (logged-in? auth)
  :post! (create-content tx/create-post params auth record)
  :return record-in-ctx)

(defdelete!
  :authorized? (can-delete-record? (record (id)) auth)
  :delete! delete-record-in-ctx)
