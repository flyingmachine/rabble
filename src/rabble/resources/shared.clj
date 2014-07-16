(ns rabble.resources.shared
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.models.permissions :refer :all]
            [rabble.db.mapification :refer :all]
            [cemerick.friend :as friend]
            [clojure.math.numeric-tower :as math]
            [flyingmachine.webutils.utils :refer :all]
            [flyingmachine.webutils.validation :refer (if-valid)]))

(defn ctx-id
  [ctx]
  (str->int (get-in ctx [:request :params :id])))

(defn params
  [ctx]
  (get-in ctx [:request :params]))

(def author-inclusion-options
  {:author {:only [:id :username :gravatar]}})

(defn auth
  [ctx]
  (friend/current-authentication (:request ctx)))

(defn ctx-logged-in?
  [ctx]
  (let [auth-user (auth ctx)]
    [(logged-in? auth-user) {:auth auth-user}]))

(defn errors-in-ctx
  [ctx]
  (select-keys ctx [:errors]))

(defn validator
  "Used in invalid? which is why truth values are reversed"
  [validation]
  (fn [ctx]
    (if-valid
     (params ctx) validation errors
     false
     [true {:errors errors
            :representation {:media-type "application/json"}}])))

(defn add-record-to-ctx
  [r]
  (if r {:record r}))

(defn exists?
  [mapification-fn]
  (fn [ctx] (add-record-to-ctx (mapification-fn (ctx-id ctx)))))

(defn record-in-ctx
  [ctx]
  (:record ctx))

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

(defn can-x-record?
  [mapification-fn predicate]
  (fn [ctx]
    (let [record (mapification-fn (ctx-id ctx))]
      (if (predicate record (auth ctx))
        {:record record}))))

(defn can-delete-record?
  [mapification-fn]
  (can-x-record? mapification-fn
                 (fn [record user]
                   (or (author? record user) (moderator? user)))))

(defn can-update-record?
  [mapification-fn]
  (can-x-record? mapification-fn
                 (fn [record user]
                   (and (not (:deleted record))
                        (or (moderator? user)
                            (author? record user))))))

(defn update-record
  [update-fn params]
  (fn [_] (update-fn params)))

(defn delete-record-in-ctx
  [ctx]
  (dj/t [{:db/id (get-in ctx [:record :id])
          :content/deleted true}]))

(defnpd create-record
  [ctx creation-fn params mapification-fn [after-create nil]]
  (let [result (creation-fn params)
        record (mapify-tx-result result mapification-fn)]
    (if after-create (after-create ctx params record))
    {:record record}))

(defnpd create-content
  [creation-fn mapification-fn [after-create nil]]
  (fn [ctx]
    (let [auth (:auth ctx)
          params (params ctx)]
      (create-record
       ctx
       creation-fn
       (merge params {:author-id (:id auth)})
       mapification-fn
       after-create))))

(def default-decisions
  (let [base {:available-media-types ["application/json"]
              :allowed-methods [:get]
              :authorized? true
              :respond-with-entity? true
              :new? false}]
    {:list base
     :create (merge base {:allowed-methods [:post]
                          :new? true
                          :handle-malformed errors-in-ctx})
     :show base
     :update base
     :delete (merge base {:allowed-methods [:delete]
                          :respond-with-entity? false})}))
