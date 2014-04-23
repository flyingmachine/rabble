(ns rabble.controllers.admin.users
  (:require [com.flyingmachine.datomic-junk :as dj]
            [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.controllers.shared :refer :all]
            [rabble.models.permissions :refer [moderator?]]
            [flyingmachine.cartographer.core :as c]
            [liberator.core :refer [defresource]]))

(def user (mapifier mr/ent->user))

(defresource query [params auth]
  :authorized? (moderator? auth)
  :available-media-types ["application/json"]
  :handle-ok (fn [_] (map user (dj/all :user/username))))
