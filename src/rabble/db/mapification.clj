(ns rabble.db.mapification
  (:require [flyingmachine.cartographer.core :as c]
            [com.flyingmachine.datomic-junk :as dj]))

(defn entify-if-id
  [to-map]
  (if (number? to-map)
    (dj/ent to-map)
    to-map))

(defn mapifier
  [rules & [mapify-opts]]
  (let [mapify-opts (or mapify-opts {})]
    (fn [to-map & [addtl-mapify-args]]
      (if-let [ent (entify-if-id to-map)]
        (let [mapify-opts (merge mapify-opts addtl-mapify-args)]
          (c/mapify ent rules mapify-opts))
        nil))))

(defn tx-result->ent
  [tx-result]
  (let [{:keys [result tempid]} tx-result]
    (-> result
        deref
        :tempids
        (dj/resolve-tempid tempid)
        dj/ent)))

(defn mapify-tx-result
  [tx-result mapifier]
  (-> tx-result
      tx-result->ent
      mapifier))
