(ns rabble.resources.watch
  (:require [datomic.api :as d]
            [rabble.db.transactions.watches :as tx]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.resources.shared :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [flyingmachine.webutils.utils :refer :all]))

(def watch (mapifier mr/ent->watch))

(defn resource-decisions
  [options defaults app-config]
  (merge-with
   merge defaults
   {:list {:handle-ok (fn [ctx]
                        (map (comp watch first)
                             (d/q '[:find ?watch
                                    :in $ ?userid
                                    :where [?watch :watch/user ?userid]
                                    [?watch :watch/topic ?topic]
                                    [?topic :content/deleted false]]
                                  (dj/db)
                                  (:id (auth ctx)))))}
    :create {:authorized? ctx-logged-in?
             :post! (fn [ctx] (create-record ctx tx/create-watch watch))
             :handle-created record-in-ctx}
    :delete {:authorized? (fn [ctx]
                            (let [watch-id (str->int (:id (params ctx)))
                                  watch (dj/ent watch-id)]
                              (if (and watch (= (:db/id (:watch/user watch))
                                                (:id (auth ctx))))
                                {:record watch-id})))
             :delete! (fn [ctx] (dj/retract (:record ctx)))}}))
