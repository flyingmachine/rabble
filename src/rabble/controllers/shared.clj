(ns rabble.controllers.shared
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.models.permissions :refer :all]
            [rabble.db.mapification :refer :all]
            [clojure.math.numeric-tower :as math]
            [flyingmachine.webutils.utils :refer :all]
            [flyingmachine.webutils.validation :refer (if-valid)]))

(def author-inclusion-options
  {:author {:only [:id :username :gravatar]}})

(defn invalid
  [errors]
  {:body {:errors errors}
   :status 412})

(defmacro id
  []
  '(str->int (:id params)))

(defmacro validator
  "Used in invalid? which is why truth values are reversed"
  [params validation]
  `(fn [ctx#]
     (if-valid
      ~params ~validation errors#
      false
      [true {:errors errors#
             :representation {:media-type "application/json"}}])))

;; working with liberator
(defn record-in-ctx
  [ctx]
  (get ctx :record))

(def exists-in-ctx? record-in-ctx)

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

(defn mapifier
  [ctx]
  (get-in ctx [:request :rabble :mapifier]))

(defn ctx-id
  [ctx]
  (str->int (get-in ctx [:request :params :id])))

(defn exists?
  [mapification-fn]
  (fn [ctx]
    (if-let [r (mapification-fn (mapifier ctx) (ctx-id ctx))]
      {:record r})))

;; TODO macro to create anonymous function with ctx stuff?

;; TODO something like slice
(defn errors-in-ctx
  [ctx]
  {:errors (get ctx :errors)})

(defn delete-record-in-ctx
  [ctx]
  (dj/t [{:db/id (get-in ctx [:record :id])
          :content/deleted true}]))

;; TODO figure out how to refactor this
(defmacro can-delete-record?
  [mapification-fn auth]
  `(fn [ctx#]
     (let [record# (~mapification-fn (mapifier ctx#) (ctx-id ctx#))
           auth# ~auth]
       (if (or (author? record# auth#) (moderator? auth#))
         {:record record#}))))

(defmacro can-update-record?
  [mapification-fn auth]
  `(fn [ctx#]
     (let [record# (~mapification-fn (mapifier ctx#) (ctx-id ctx#))
           auth# ~auth]
       (if (and (not (:deleted record#))
                (or (moderator? auth#)
                    (author? record# auth#)))
         {:record record#}))))

(defn update-record
  [params update-fn]
  (fn [_]
    (update-fn params)))

(defn create-record
  [creation-fn params mapification-fn]
  (fn [ctx]
    (let [result (creation-fn params)]
      {:record (mapify-tx-result result (partial mapification-fn (mapifier ctx)))})))

(defn create-content
  [creation-fn params auth mapification-fn]
  (create-record creation-fn (merge params {:author-id (:id auth)}) mapification-fn))

(defn mapify-with
  [mapify-fn]
  (fn [ctx]
    (mapify-fn (mapifier ctx) (ctx-id ctx))))
