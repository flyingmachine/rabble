(ns rabble.middleware.mapifier)

(deftype RabbleMapifier [])

(defn add-rabble-mapifier
  [handler]
  (let [mapifier (RabbleMapifier.)]
    (fn [request]
      (handler
       (if-not (get-in request [:rabble :mapifier])
         (merge-with merge request {:rabble {:mapifier mapifier}}))))))
