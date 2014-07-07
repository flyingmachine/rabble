(ns rabble.resources.topic
  (:require [datomic.api :as d]
            [rabble.db.validations :as validations]
            [com.flyingmachine.datomic-junk :as dj]
            [rabble.db.maprules :as mr]
            [rabble.db.transactions.topics :as topic-tx]
            [rabble.db.transactions.watches :as watch-tx]
            [liberator.core :refer [defresource]]
            [rabble.resources.shared :refer :all]
            [rabble.models.permissions :refer :all]
            [rabble.db.mapification :refer :all]
            [rabble.email.sending.notifications :as n]
            [rabble.config :refer (config)]
            [flyingmachine.webutils.utils :refer :all]
            [com.flyingmachine.liberator-templates.sets.json-crud
             :refer (defquery defshow defcreate! defdelete!)]))

(def list-topic
  (mapifier
   mr/ent->topic
   {:include (merge {:first-post {:only [:id :likers :content]
                                  :include {:author {:only [:display-name]}}}
                     :tags {}}
                    author-inclusion-options)}))

(def topic
  (mapifier
   mr/ent->topic
   {:include {:posts {:include author-inclusion-options}
              :tags {}
              :watches {}}}))

(defn organize
  "Topics come in [eid date] pairs"
  [topics]
  (map first (reverse-by second topics)))

(def base-query '[:find ?e ?t
                  :where
                  [?e :topic/last-posted-to-at ?t]
                  [?e :content/deleted false]])

(defn tag-ids
  [tags]
  (map str->int (clojure.string/split tags #",")))

(defn tag-conditions
  [tag-ids]
  (map (fn [id] ['?e :content/tags id]) tag-ids))

(defn build-query
  [params]
  (if-let [tags (:tags params)]
    (into base-query (-> tags tag-ids tag-conditions))
    base-query))

(defn all
  [params]
  (organize (d/q (build-query params) (dj/db))))

(defn create-resource-configs
  [options defaults app-config]
  (merge-with
   merge defaults
   {:list {:handle-ok (fn [{{params :params} :request}]
                        (mapify-rest
                         (-> options :list :mapifier)
                         (paginate (all params) (or (app-config :per-page) 20) params)))}
    :create {:authorized? ctx-logged-in?
             :invalid? (validator (-> options :create :validation))
             :post! (create-content
                     topic-tx/create-topic
                     (-> options :list :mapifier)
                     (-> options :create :after-create))
             :handle-created record-in-ctx}}))
