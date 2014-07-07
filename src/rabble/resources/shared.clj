(ns rabble.resources.shared
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.models.permissions :refer :all]
            [rabble.db.mapification :refer :all]
            [clojure.math.numeric-tower :as math]
            [flyingmachine.webutils.utils :refer :all]
            [flyingmachine.webutils.validation :refer (if-valid)]))

(def author-inclusion-options
  {:author {:only [:id :username :gravatar]}})

(defn validator
  [params validation]
  "Used in invalid? which is why truth values are reversed"
  (fn [ctx]
    (if-valid
     params validation errors
     false
     [true {:errors errors
            :representation {:media-type "application/json"}}])))

(defn record-in-ctx
  [ctx]
  (get ctx :record))

(def exists-in-ctx? record-in-ctx)

(defn mapify-rest
  [map-fn ents]
  (conj (map map-fn (rest ents))
        (first ents)))

(defn paginate
  [ents per-page params]
  (let [ent-count (count ents)
        page-count (math/ceil (/ ent-count per-page))
        current-page (or (str->int (:page params)) 1)
        skip (* (dec current-page) per-page)
        paged-ents (take per-page (drop skip ents))]
    (conj paged-ents {:page-count page-count
                      :ent-count ent-count
                      :current-page current-page})))

(defn params
  [ctx]
  (get-in ctx [:request :params]))

(defn update-record
  [update-fn params]
  (fn [_] (update-fn params)))

(defnpd create-record
  [creation-fn params mapification-fn [after-create nil]]
  (fn [ctx]
    (let [result (creation-fn params)
          record (mapify-tx-result result mapification-fn)]
      (if after-create (after-create ctx params record))
      {:record record})))

(defnpd create-content
  [creation-fn params auth mapification-fn [after-create nil]]
  (create-record creation-fn (merge params {:author-id (:id auth)}) mapification-fn after-create))
