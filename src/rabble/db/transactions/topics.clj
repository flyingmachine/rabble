(ns rabble.db.transactions.topics
  (:require [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.email.sending.senders :as email]
            [flyingmachine.webutils.utils :refer :all]
            [clojure.string :refer (split trim lower-case)]))

(def topic-params->txdata (comp remove-nils-from-map (mapifier mr/topic->txdata)))
(def watch-params->txdata (mapifier mr/watch->txdata))
(def post-params->txdata (mapifier mr/post->txdata))

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

;; TODO possibly generalize this by accepting x-params->txdata
;; functions whic append to transaction
(defn create-topic
  [params]
  (let [final-params (add-create-params params)
        result {:result (-> final-params
                            params->tags
                            (into (topic-transaction-data final-params))
                            dj/t)
                :tempid (:topic-id final-params)}]
    result))
