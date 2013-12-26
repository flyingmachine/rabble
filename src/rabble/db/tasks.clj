(ns rabble.db.tasks
  (:gen-class)
  (:require [datomic.api :as d]
            [com.flyingmachine.datomic-junk :as dj]
            [clojure.java.io :as io]
            [flyingmachine.webutils.utils :refer :all]
            [rabble.db.schema :as schema]
            [rabble.config :refer (config)]))

(defn create
  []
  (d/create-database dj/*db-uri*))

(defn delete
  []
  (d/delete-database dj/*db-uri*))

(defn recreate
  []
  (delete)
  (create))

(def rabble-schemas
  [:20130521-161013-schema
   :20130521-161014-seed-data
   :20130807-183200-tags
   :20131003-111111-user-prefs
   :20131018-000000-password-reset
   :20131021-000000-topic-privacy])

(def schema-attr (or (config :schema-attr) :rabble/schema))

(defn schema-path
  [schema-name]
  (str "schemas/" (name schema-name) ".edn"))

(defn schema-data
  [schema-name]
  {:txes [(-> schema-name
              schema-path
              read-resource)]})

(defn schema-map
  [schema-names]
  (reduce (fn [m name]
            (assoc m name (schema-data name)))
          {}
          schema-names))

(defn install-schemas
  ([]
     (install-schemas rabble-schemas))
  ([schemas]
     (apply schema/ensure-schemas
            (into [(dj/conn) schema-attr (schema-map rabble-schemas)] schemas))))

(defn rename-schemas
  [name-map]
  (let [schemas (dj/all schema-attr)]
    (filter identity (map (fn [schema]
                            (if-let [new-name (get name-map (get schema schema-attr))]
                              [:db/add (:db/id schema) schema-attr new-name]))
                          schemas))))

{:20130521-161013-schema :base
 :20130521-161014-seed-data :watch-count-fn
 :20130807-183200-tags :tags
 :20131003-111111-user-prefs :user-prefs
 :20131018-000000-password-reset :password-reset
 :20131021-000000-topic-privacy :topic-privacy}

(defn seed
  []
  (dj/t (read-resource "fixtures/seeds.edn")))

(defn reload
  []
  (recreate)
  (install-schemas))
