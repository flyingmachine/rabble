(ns rabble.db.transactions.topics
  (:require [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.email.sending.senders :as email]
            [flyingmachine.cartographer.core :as c]
            [flyingmachine.webutils.utils :refer :all]
            [clojure.string :refer (split trim lower-case)]))

(defmapifier record mr/ent->topic {:include [:first-post]})

(defmapifier topic-params->txdata* mr/topic->txdata)
(def topic-params->txdata (comp remove-nils-from-map topic-params->txdata*))

(defmapifier watch-params->txdata mr/watch->txdata)

(defmapifier post-params->txdata mr/post->txdata)

;: TODO refactor with post notification query
(defn users-to-notify-of-topic
  [author-id]
  (dj/ents (dj/q (conj '[:find ?u :where]
                       '[?u :user/preferences "receive-new-topic-notifications"]
                       [(list 'not= '?u author-id)]))))

(defn- notify-users-of-topic
  [result params]
  (let [{:keys [topic-id author-id]} params
        users (users-to-notify-of-topic author-id)
        topic (mapify-tx-result result record)]
    (email/send-new-topic-notification users topic)))

(defn- after-create-topic
  [result params]
  (future (notify-users-of-topic result params)))

(defn- add-create-params
  [params]
  (merge params (dj/tempids :topic-id :post-id :watch-id)))

(defn- topic-transaction-data
  [params]
  (map #(% params) [topic-params->txdata post-params->txdata watch-params->txdata]))

(defn tags
  [tag-string]
  (->> (split (or tag-string "") #",")
       (map trim)
       (filter not-empty)
       (reduce (fn [result tag-name]
                 (merge-with conj result
                             (if-let [tag (dj/one [:tag/name tag-name])]
                               {:tag-ids (:db/id tag)}
                               (let [tempid (d/tempid :db.part/user)]
                                 {:tag-ids tempid
                                  :new-tags {:tag/name tag-name
                                             :db/id tempid}}))))
               {:tag-ids []
                :new-tags []})))

(defn params->tags
  [params]
  (let [{:keys [tag-ids new-tags]} (tags (:tags params))]
    (into [[:db/add (:topic-id params) :content/tags tag-ids]]
          new-tags)))

(defn create-topic
  [params]
  (let [final-params (add-create-params params)
        result {:result (-> final-params
                            params->tags
                            (into (topic-transaction-data final-params))
                            dj/t)
                :tempid (:topic-id final-params)}]
    (after-create-topic result final-params)
    result))
