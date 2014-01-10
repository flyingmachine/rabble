(ns rabble.middleware.extend-mapifier
  (:require [rabble.controllers.topics :refer (TopicsController)]
            [rabble.controllers.posts :refer (PostsController)]
            [rabble.controllers.users :refer (UsersController)]
            [rabble.email.sending.notifications :refer (Notifications)])
  (:import [rabble.middleware.mapifier RabbleMapifier]))

(defn default-impl
  [protocol]
  (get-in protocol [:impls RabbleMapifier]))

(def defaults (reduce #(into %1 [%2 (default-impl %2)]) []
                      [TopicsController PostsController UsersController Notifications]))

(defn extend-defaults
  [x]
  (apply extend x defaults))
