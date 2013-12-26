(ns rabble.db.transactions.t-posts
  (:require [rabble.test.db-helpers :as tdb]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.transactions.posts :as p]
            [rabble.db.transactions.topics :as t]
            [rabble.db.transactions.watches :as w])
  (:use midje.sweet
        rabble.test.controller-helpers))

(setup-db-background
 (t/create-topic {:author-id (:id (auth))
                            :title "test topic"
                            :content "test"}))

(defn topic
  []
  (dj/one [:topic/title "test topic"]))

(defn create-post
  ([params]
     (let [defaults {:topic-id (:db/id (topic))
                     :author-id (:id (auth))
                     :content "here's some content"}
           params (merge defaults params)]
       (p/create-post params)))
  ([]
     (create-post {})))

(defmapifier post mr/ent->post)

(fact "create-post creates a post"
  (create-post)
  => (just {:result anything
            :tempid anything}))

(fact "update-post updates a post"
  (let [post-id (:db/id (tx-result->ent (create-post)))]
    (p/update-post {:id post-id
                    :content "new content"})
    (:post/content (dj/ent post-id))
    => "new content"))

(fact "users-to-notify-of-post returns a correct seq of users"
  (let [topic-id (:db/id (topic))
        user-id (:id (auth "joebob"))
        author-id (:id (auth))]
    (w/create-watch {:topic-id topic-id
                     :user-id user-id})
    (p/users-to-notify-of-post topic-id author-id)
    => []))
