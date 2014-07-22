(ns rabble.config
  (require [com.flyingmachine.config :as config]
           [environ.core]))

(config/defconfig
  config
  (merge-with merge
              {:rabble {:per-page 50}}
              environ.core/env)
  :rabble)
