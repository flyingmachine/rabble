(ns rabble.controllers.tags
  (:require [rabble.db.maprules :as mr]
            [flyingmachine.webutils.utils :refer :all]
            [com.flyingmachine.datomic-junk :as dj])
  (:use [liberator.core :only [defresource]]
        rabble.controllers.shared
        rabble.db.mapification))

(defmapifier record mr/ent->tag)

(defresource query [params]
  :available-media-types ["application/json"]
  :handle-ok (fn [_]
               (sort-by :name
                        (map record
                             (dj/all :tag/name)))))
