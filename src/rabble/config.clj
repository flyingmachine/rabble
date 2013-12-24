(ns rabble.config
  (require [com.flyingmachine.config :as config]))

(config/defconfig config environ.core/env :rabble)
