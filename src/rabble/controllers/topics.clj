(ns rabble.controllers.topics
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.maprules :as mr]
            [rabble.db.transactions.topics :as topic-tx]
            [rabble.db.transactions.watches :as watch-tx]
            [liberator.core :refer [defresource]]
            [rabble.controllers.shared :refer :all]
            [rabble.models.permissions :refer :all]
            [rabble.db.mapification :refer :all]
            [rabble.lib.dispatcher :refer :all]
            [rabble.email.sending.notifications :as n]
            [rabble.config :refer (config)]
            [flyingmachine.webutils.utils :refer :all]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defquery defshow defcreate! defdelete!)])
  (:import [rabble.lib.dispatcher RabbleDispatcher]))

(defprotocol TopicsController
  (query-record [dispatcher ent])
  (record [dispatcher ent]))

(defmapifier query-record*
  mr/ent->topic
  {:include (merge {:first-post {:only [:id :likers :content]
                                 :include {:author {:only [:display-name]}}}
                    :tags {}}
                   author-inclusion-options)})

(defmapifier record*
  mr/ent->topic
  {:include {:posts {:include author-inclusion-options}
             :tags {}
             :watches {}}})

(extend-type RabbleDispatcher
  TopicsController
  (query-record [dispatcher ent] (query-record* ent))
  (record [dispatcher ent] (record* ent)))

(defn organize
  "Topics come in [eid date] pairs"
  [topics]
  (map first (reverse-by second topics)))

(def base-query '[:find ?e ?t
                  :where
                  [?e :topic/last-posted-to-at ?t]
                  [?e :content/deleted false]])

(defn tag-ids
  [tags]
  (map str->int (clojure.string/split tags #",")))

(defn tag-conditions
  [tag-ids]
  (map (fn [id] ['?e :content/tags id]) tag-ids))

(defn build-query
  [params]
  (if-let [tags (:tags params)]
    (into base-query (-> tags tag-ids tag-conditions))
    base-query))

(defn all
  [params]
  (organize (d/q (build-query params) (dj/db))))

(defquery
  :return (fn [ctx]
            (mapify-rest
             (dispatcher ctx)
             query-record
             (paginate (all params) (or (config :per-page) 50) params))))

(defshow
  :exists? (exists? record)
  :return (fn [ctx]
            (if auth (watch-tx/reset-watch-count (id) (:id auth)))
            (record-in-ctx ctx)))

(defcreate!
  :authorized? (logged-in? auth)
  :invalid? (validator params validations/topic)
  :post! (create-content
          topic-tx/create-topic params auth query-record
          (fn [ctx params topic]
            (future (n/notify-users-of-topic (dispatcher ctx) topic params))))
  :return record-in-ctx)

(defdelete!
  :authorized? (can-delete-record? record auth)
  :delete! delete-record-in-ctx)
