(ns rabble.resources.generate
  (:require [liberator.core :refer [resource]]
            [com.flyingmachine.config :as config]))

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
      (if (fn? entry)
        (entry ctx)
        entry))))

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

(defn resource-for-keys
  [resource-configs & keys]
  (let [resource-config (select-keys resource-configs keys)]
    (condp = (count resource-config)
      0 nil
      1 (apply resource (first (vals resource-config)))
      2 (apply resource (combine-configs resource-config)))))

(defn entry-resource
  [resource-configs]
  (resource-for-keys resource-configs :show :update :delete))

(defn collection-resource
  [resource-configs]
  (resource-for-keys resource-configs :list :create))

(defn resources
  [resource-key create-resource-configs options]
  (let [resource-configs (create-resource-configs (resource-key options) {})]
    {:collection (collection-resource resource-configs)
     :entry (entry-resource resource-configs)}))
