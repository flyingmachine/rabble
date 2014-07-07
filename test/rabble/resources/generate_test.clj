(ns rabble.resources.generate-test
  (:require [rabble.resources.generate :as g])
  (:use midje.sweet))

(facts "method-resource-map"
  (fact "associates an http method with a resource key"
    (g/method-resource-map {:list {:allowed-methods [:get]}
                            :create {:allowed-methods [:post]}})
    => {:get :list
        :post :create})
  (fact "provides :get by default"
    (g/method-resource-map {:list {}})
    => {:get :list}))

(let [combined (g/combine-configs {:list {:allowed-methods [:get]}
                                   :create {:allowed-methods [:post]}})]
  (fact "combine configs handles allowed methods correctly"
    (:allowed-methods combined) => #{:get :post}))
