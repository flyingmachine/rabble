(ns rabble.resources.generate
  (:require [liberator.core :refer [resource default-functions]]
            [compojure.core :refer (ANY routes)]
            [rabble.config :as app-config]
            [rabble.resources.shared :as shared]))

(def todo nil)

(def resource-options
  {:post {:list {:mapifier todo}
          :create {:validation-rules todo
                   :authorized? todo}
          :update {:validation-rules todo}}})

(defn method-resource-map
  [config-map]
  (reduce (fn [result [k v]]
            (assoc result (or (first (:allowed-methods v)) :get) k))
          {}
          config-map))

(defn method-dispatcher
  "Returns a function which calls the correct option based on the
  request method"
  [config-map method-map option]
  (fn [ctx]
    (let [request-method (get-in ctx [:request :request-method])
          resource-key (request-method method-map)
          entry (get-in config-map [resource-key option])]
      (cond
       (fn? entry) (entry ctx)
       (nil? entry) (option default-functions)
       :else entry))))

(defn combine-configs
  [config-map]
  (let [configs (vals config-map)
        options (disj (set (mapcat keys configs)) :allowed-methods)
        seed {:allowed-methods (set (mapcat :allowed-methods configs))}
        method-map (method-resource-map config-map)]
    (reduce (fn [config option]
              (assoc config option (method-dispatcher config-map method-map option)))
            seed
            options)))

(defn config->resource
  [config]
  (apply resource (apply concat config)))

(defn resource-for-keys
  [resource-configs & keys]
  (let [resource-config (select-keys resource-configs keys)
        config-count (count resource-config)]
    (cond
     (= 0 config-count) nil
     (= 1 config-count) (config->resource (first (vals resource-config)))
     :else (config->resource (combine-configs resource-config)))))

(defn entry-resource
  [resource-configs]
  (resource-for-keys resource-configs :show :update :delete))

(defn collection-resource
  [resource-configs]
  (resource-for-keys resource-configs :list :create))

(defn generate-resources
  [resource-decision-generator decision-options decision-defaults app-config]
  (let [resource-configs (resource-decision-generator decision-options decision-defaults app-config)]
    {:collection (collection-resource resource-configs)
     :entry (entry-resource resource-configs)}))

(defn resource-route
  [path
   resource-decision-generator
   & {:keys [decision-options decision-defaults entry-key app-config]
      :or {decision-options {}
           decision-defaults shared/default-decisions
           entry-key ":id"
           app-config app-config/config}}]
  (let [resources (generate-resources resource-decision-generator
                                      decision-options
                                      decision-defaults
                                      app-config)]
    (routes
     (ANY path [] (:collection resources))
     (ANY (str path "/" entry-key) [] (:entry resources)))))
