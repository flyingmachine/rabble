(ns rabble.db.transactions.posts
  (:require [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.email.sending.senders :as email]
            [flyingmachine.cartographer.core :as c]
            [flyingmachine.webutils.utils :refer :all]))

(def post-params->txdata* (mapifier mr/post->txdata))
(def post-params->txdata (comp remove-nils-from-map post-params->txdata*))
(def watch-params->txdata (mapifier mr/watch->txdata))

(defn- add-create-params
  [params]
  (merge params (dj/tempids :watch-id)))

(defn watch
  [topic-id author-id]
  (dj/one [:watch/topic topic-id] [:watch/user author-id]))

(defn post-transaction
  [params post topic-id author-id]
  (let [t [post
           {:db/id topic-id
            :topic/last-posted-to-at (:post/created-at post)
            :topic/last-post (:db/id post)}
           [:increment-watch-count topic-id author-id]]]
    (if (watch topic-id author-id)
      t
      (conj t (watch-params->txdata params)))))

(defn create-post
  [params]
  (let [final-params (add-create-params params)
        {:keys [topic-id author-id]} final-params
        post (post-params->txdata final-params)
        result {:result (dj/t (post-transaction final-params post topic-id author-id))
                :tempid (:db/id post)}]
    result))

(defn update-post
  [params]
  (dj/t [(c/mapify params mr/post->txdata {:only [:db/id :post/content]})]))
