(ns rabble.resources.stats
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.shared :refer :all]))

(defn resource-decisions
  [options defaults app-config]
  (merge-decision-defaults
   {:list {:handle-ok (fn [_]
                        {:topic-count (dj/ent-count :topic/title)
                         :post-count (dj/ent-count :post/content)
                         :like-count (dj/ent-count :like/user)
                         :user-count (dj/ent-count :user/username)})}}
   defaults))
