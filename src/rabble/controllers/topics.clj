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
  {:include (merge {:first-post {:only [:id :likers :content]
                                 :include {:author {:only [:display-name]}}}
                    :tags {}}
                   author-inclusion-options)})

(defmapifier query-record
  mr/ent->topic
  query-mapify-options)

(defmapifier record
  mr/ent->topic
  {:include {:posts {:include author-inclusion-options}
             :tags {}
             :watches {}}})

(defn organize
  "Topics come in [eid date] pairs"
  [topics]
  (map first (reverse-by second topics)))

(def base-query '[:find ?e ?t
                  :where
                  [?e :topic/last-posted-to-at ?t]
                  [?e :content/deleted false]])

(defn all
  []
  (organize
   (d/q base-query (dj/db))))

(defn tagged
  [tags]
  (let [tag-conditions (map (fn [tag] ['?e :content/tags tag]) tags)]
    (organize
     (d/q (into base-query tag-conditions)
          (dj/db)))))

(defn mapify
  [topics]
  (map query-record topics))

(defn paginate
  [topics params]
  (let [per-page 50
        topic-count (count topics)
        page-count (math/ceil (/ topic-count per-page))
        current-page (or (str->int (:page params)) 1)
        skip (* (dec current-page) per-page)
        paged-topics (mapify (take per-page (drop skip topics)))]
    (conj paged-topics {:page-count page-count
                        :topic-count topic-count
                        :current-page current-page})))

(defquery
  :return (fn [_]
               (paginate
                (if-let [tags (:tags params)]
                  (tagged (map str->int (clojure.string/split tags #",")))
                  (all))
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
