(ns rabble.middleware.dispatcher
  (:require [rabble.lib.dispatcher]))

(defn add-rabble-dispatcher
  [dispatcher]
  (fn [handler]
    (fn [request]
      (handler
       (if-not (get-in request [:rabble :dispatcher])
         (merge-with merge request {:rabble {:dispatcher dispatcher}}))))))
