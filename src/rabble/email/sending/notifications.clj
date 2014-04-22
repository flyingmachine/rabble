(ns rabble.email.sending.notifications
  (:require [rabble.email.sending.senders :as email]
            [rabble.db.mapification :refer (defmapifier)]
            [rabble.db.maprules :as mr]
            [com.flyingmachine.datomic-junk :as dj]
            [flyingmachine.cartographer.core :as c]
            rabble.lib.dispatcher))

(defmapifier author mr/ent->user {:only [:email :username :display-name]})

(defn users
  ([author-id preference]
     (users author-id preference []))
  ([author-id preference conditions]
     (dj/ents (dj/q {:find '[?u]
                     :where (into conditions
                                  [['?u :user/preferences preference]
                                   [(list 'not= '?u author-id)]])}))))

;: TODO refactor with post notification query
(defn users-to-notify-of-topic
  [author-id]
  (users author-id "receive-new-topic-notifications"))

(defn notify-users-of-topic
  [topic params]
  (let [{:keys [author-id]} params
        users (users-to-notify-of-topic author-id)]
    (email/send-new-topic-notification users topic (author author-id))))

(defn users-to-notify-of-post
  [topic-id author-id]
  (users author-id
         "receive-watch-notifications"
         [['?w :watch/topic topic-id]
          ['?w :watch/user '?u]]))

(defn notify-users-of-post
  [params]
  (let [{:keys [topic-id author-id]} params
        users (users-to-notify-of-post topic-id author-id)
        topic (c/mapify (dj/ent topic-id) mr/ent->topic)]
    (email/send-reply-notification users topic (author author-id) params)))
