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

(defn validator
  [params validation]
  "Used in invalid? which is why truth values are reversed"
  (fn [ctx]
    (if-valid
     params validation errors
     false
     [true {:errors errors
            :representation {:media-type "application/json"}}])))

;; working with liberator
(defn record-in-ctx
  [ctx]
  (get ctx :record))

(def exists-in-ctx? record-in-ctx)

(defn mapify-rest
  [mapifier map-fn ents]
  (conj (map (partial map-fn mapifier) (rest ents))
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

(defn dispatcher
  [ctx]
  (get-in ctx [:request :rabble :dispatcher]))

(defn ctx-id
  [ctx]
  (str->int (get-in ctx [:request :params :id])))

(defn add-record-to-ctx
  [r]
  (if r {:record r}))

(defn exists?
  [mapification-fn]
  (fn [ctx] (add-record-to-ctx (mapification-fn (dispatcher ctx) (ctx-id ctx)))))

(defn errors-in-ctx
  [ctx]
  (select-keys ctx [:errors]))

(defn delete-record-in-ctx
  [ctx]
  (dj/t [{:db/id (get-in ctx [:record :id])
          :content/deleted true}]))

;; TODO figure out how to refactor this
(defmacro can-delete-record?
  [mapification-fn auth]
  `(fn [ctx#]
     (let [record# (~mapification-fn (dispatcher ctx#) (ctx-id ctx#))
           auth# ~auth]
       (if (or (author? record# auth#) (moderator? auth#))
         {:record record#}))))

(defmacro can-update-record?
  [mapification-fn auth]
  `(fn [ctx#]
     (let [record# (~mapification-fn (dispatcher ctx#) (ctx-id ctx#))
           auth# ~auth]
       (if (and (not (:deleted record#))
                (or (moderator? auth#)
                    (author? record# auth#)))
         {:record record#}))))

(defn update-record
  [params update-fn]
  (fn [_]
    (update-fn params)))

(defnpd create-record
  [creation-fn params mapification-fn [after-create nil]]
  (fn [ctx]
    (let [result (creation-fn params)
          record (mapify-tx-result result (partial mapification-fn (dispatcher ctx)))]
      (if after-create (after-create ctx params record))
      {:record record})))

(defnpd create-content
  [creation-fn params auth mapification-fn [after-create nil]]
  (create-record creation-fn (merge params {:author-id (:id auth)}) mapification-fn after-create))

(defn mapify-with
  [mapify-fn]
  (fn [ctx]
    (mapify-fn (dispatcher ctx) (ctx-id ctx))))
