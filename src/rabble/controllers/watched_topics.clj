(ns rabble.controllers.watched-topics
  (:require [com.flyingmachine.datomic-junk :as dj]
            [datomic.api :as d]
            [rabble.db.maprules :as mr]
            rabble.lib.dispatcher
            [flyingmachine.cartographer.core :as c]
            [cemerick.friend :as friend]
            [rabble.config :refer (config)])
  (:use [liberator.core :only [defresource]]
        rabble.controllers.shared
        rabble.models.permissions
        rabble.db.mapification
        flyingmachine.webutils.utils)
  (:import [rabble.lib.dispatcher RabbleDispatcher]))

(defprotocol WatchedTopicsController
  (query-record [dispatcher ent])
  (record [dispatcher ent]))

(defmapifier record*
  mr/ent->topic
  {:include (merge {:first-post {}}
                   author-inclusion-options)})

(extend-type RabbleDispatcher
  WatchedTopicsController
  (record [dispatcher ent] (record* ent)))

(defn ents
  [auth]
  (map first
       (d/q '[:find ?topic
              :in $ ?userid
              :where [?watch :watch/user ?userid]
              [?watch :watch/topic ?topic]
              [?topic :content/deleted false]]
            (dj/db)
            (:id auth))))

(defresource query [params auth]
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
               (mapify-rest
                (dispatcher ctx)
                record
                (paginate (reverse-by :db/last-posted-to-at (ents auth))
                          (or (config :per-page) 50)
                          params))))
