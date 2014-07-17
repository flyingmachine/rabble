(ns rabble.resources.tag
  (:require [rabble.db.maprules :as mr]
            [flyingmachine.webutils.utils :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defquery)])
  (:use rabble.resources.shared
        rabble.db.mapification))

(def tag (mapifier mr/ent->tag))

(defn resource-decisions
  [options defaults app-config]
  (merge-decision-defaults
   {:list {:handle-ok (fn [_]
                        (->> :tag/name
                             dj/all
                             (map (-> options :list :mapifier))
                             (sort-by :name)))}}
   defaults))

(def default-options
  {:list {:mapifier tag}})
