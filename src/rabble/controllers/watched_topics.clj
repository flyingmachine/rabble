(ns rabble.controllers.watched-topics
  (:require [com.flyingmachine.datomic-junk :as dj]
            [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [flyingmachine.cartographer.core :as c]
            [cemerick.friend :as friend]
            [rabble.config :refer (config)])
  (:use [liberator.core :only [defresource]]
        rabble.controllers.shared
        rabble.models.permissions
        rabble.db.mapification
        flyingmachine.webutils.utils))

(defmapifier record
  mr/ent->topic
  {:include (merge {:first-post {}}
                   author-inclusion-options)})

(defn ents
  [auth]
  (map (comp dj/ent first)
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
                record
                (paginate (reverse-by :topic/last-posted-to-at (ents auth))
                          (or (config :per-page) 50)
                          params))))
