(ns rabble.db.transactions.posts
  (:require [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.email.sending.senders :as email]
            [flyingmachine.cartographer.core :as c]
            [flyingmachine.webutils.utils :refer :all]))

(defmapifier post-params->txdata* mr/post->txdata)
(def post-params->txdata (comp remove-nils-from-map post-params->txdata*))
(defmapifier watch-params->txdata mr/watch->txdata)

(defn- add-create-params
  [params]
  (merge params (dj/tempids :watch-id)))

(defn create-post
  [params]
  (let [final-params (add-create-params params)
        {:keys [topic-id author-id]} final-params
        post (post-params->txdata final-params)
        result {:result (dj/t [post
                               {:db/id topic-id
                                :topic/last-posted-to-at (:post/created-at post)}
                               (watch-params->txdata final-params)
                               [:increment-watch-count topic-id author-id]])
                :tempid (:db/id post)}]
    result))

(defn update-post
  [params]
  (dj/t [(c/mapify params mr/post->txdata {:only [:db/id :post/content]})]))
