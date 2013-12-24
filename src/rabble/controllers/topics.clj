(ns rabble.controllers.topics
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.maprules :as mr]
            [rabble.db.transactions.topics :as topic-tx]
            [rabble.db.transactions.watches :as watch-tx]
            [clojure.math.numeric-tower :as math]
            [liberator.core :refer [defresource]]
            [rabble.controllers.shared :refer :all]
            [rabble.models.permissions :refer :all]
            [rabble.db.mapification :refer :all]
            [flyingmachine.webutils.utils :refer :all]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defquery defshow defcreate! defdelete!)]))

(def query-mapify-options
  {:include (merge {:first-post {:only [:content :likers :id]}}
                   author-inclusion-options)})

(defmapifier query-record mr/ent->topic query-mapify-options)

(defmapifier record
  mr/ent->topic
  {:include {:posts {:include author-inclusion-options}
             :watches {}}})


(defn paginate
  [topics params]
  (let [per-page 15
        topic-count (count topics)
        page-count (math/ceil (/ topic-count per-page))
        current-page (or (str->int (:page params)) 1)
        skip (* (dec current-page) per-page)
        paged-topics (take per-page (drop skip topics))]
    (conj paged-topics {:page-count page-count :topic-count topic-count})))

(defn visibility
  [auth]
  (when-not (logged-in? auth)
    [:topic/visibility :visibility/public]))

(defquery
  :return (fn [_]
               (paginate
                (reverse-by :last-posted-to-at
                            (map query-record
                                 (dj/all :topic/first-post
                                         [:content/deleted false]
                                         (visibility auth))))
                params)))

(defshow
  :exists? (exists? (record (id)))
  :handle-ok (fn [ctx]
               (if auth (watch-tx/reset-watch-count (id) (:id auth)))
               (record-in-ctx ctx)))

(defcreate!
  :authorized? (logged-in? auth)
  :invalid? (validator params validations/topic)
  :post! (create-content topic-tx/create-topic params auth query-record)
  :return record-in-ctx)

(defdelete!
  :authorized? (can-delete-record? (record (id)) auth)
  :delete! delete-record-in-ctx)
