(ns rabble.lib.extend-dispatcher
  (:require rabble.lib.dispatcher
            [rabble.controllers.topics :refer (TopicsController)]
            [rabble.controllers.watched-topics :refer (WatchedTopicsController)]
            [rabble.controllers.posts :refer (PostsController)]
            [rabble.controllers.users :refer (UsersController)]
            [rabble.email.sending.notifications :refer (Notifications)])
  (:import [rabble.lib.dispatcher RabbleDispatcher]))

(defn default-impl
  [protocol]
  (get-in protocol [:impls RabbleDispatcher]))

(def defaults (reduce #(into %1 [%2 (default-impl %2)]) []
                      [TopicsController
                       WatchedTopicsController
                       PostsController
                       UsersController
                       Notifications]))

;; TODO somehow write a merge-with merge way of extending
(defn extend-defaults
  [x]
  (apply extend x defaults))
