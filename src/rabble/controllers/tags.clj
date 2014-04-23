(ns rabble.controllers.tags
  (:require [rabble.db.maprules :as mr]
            [flyingmachine.webutils.utils :refer :all]
            [com.flyingmachine.datomic-junk :as dj]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defquery)])
  (:use rabble.controllers.shared
        rabble.db.mapification))

(def tag (mapifier mr/ent->tag))

(defquery [params]
  :return (fn [_]
            (->> :tag/name
                 dj/all
                 (map tag)
                 (sort-by :name))))
