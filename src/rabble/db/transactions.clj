(ns rabble.db.transactions
  (:require [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.mapification :refer [mapify-tx-result]]
            [flyingmachine.cartographer.core :as c]
            [flyingmachine.webutils.utils :refer :all]))

(defn create-user
  [params]
  (let [params (merge {:user/preferences ["receive-watch-notifications" "receive-new-topic-notifications" "receive-weekly-digest"] }
                      (remove-nils-from-map (c/mapify params mr/user->txdata)))]
    {:result (dj/t [params])
     :tempid (:db/id params)}))

