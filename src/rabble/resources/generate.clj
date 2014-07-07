(ns rabble.resources.generate
  (:require [liberator.core :refer [resource]]
            [com.flyingmachine.config :as config]))

(def todo nil)

(def resource-options
  {:post {:list {:mapifier todo}
          :create {:validation-rules todo
                   :authorized? todo}
          :update {:validation-rules todo}}})

(defn resources
  [resource-key resource-config options]
  (apply resource (:list (resource-config (resource-key options) {}))))
