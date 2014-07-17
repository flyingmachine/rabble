(ns rabble.resources.watched-topic
  (:require [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.resources.shared :refer :all]
            [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj]
            [flyingmachine.webutils.utils :refer :all]))

(def topic (mapifier
            mr/ent->topic
            {:include (merge {:first-post {}}
                             author-inclusion-options)}))

(defn ents
  [auth]
  (map (comp dj/ent first)
       (d/q '[:find ?topic
              :in $ ?userid
              :where [?watch :watch/user ?userid]
              [?watch :watch/topic ?topic]
              [?topic :content/deleted false]]
            (dj/db)
            (:id auth))))

(defn resource-decisions
  [options defaults app-config]
  (merge-decision-defaults
   {:list {:handle-ok (fn [ctx]
                        (mapify-rest
                         (-> options :list :mapifier)
                         (paginate (reverse-by :topic/last-posted-to-at (ents (auth ctx)))
                                   (or (app-config :per-page) 50)
                                   (params ctx))))}}
   defaults))
