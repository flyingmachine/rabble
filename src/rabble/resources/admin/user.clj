(ns rabble.resources.admin.user
  (:require [com.flyingmachine.datomic-junk :as dj]
            [datomic.api :as d]
            [rabble.db.maprules :as mr]
            [rabble.db.mapification :refer :all]
            [rabble.resources.shared :refer :all]
            [rabble.models.permissions :refer [moderator?]]
            [liberator.core :refer [defresource]]))

(def user (mapifier mr/ent->user))

(defn resource-decisions
  [options defaults app-config]
  (merge-decision-defaults
   defaults
   {:list {:authorized? (fn [ctx] (moderator? (auth ctx)))
           :handle-ok (fn [_] (map (-> options :list :mapifier) (dj/all :user/username)))}}))

(def default-options
  {:list {:mapifier user}})
