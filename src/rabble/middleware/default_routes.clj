(ns rabble.middleware.default-routes
  (:require compojure.route
            [compojure.core :refer (GET PUT POST DELETE ANY defroutes routes)]
            [ring.util.response :as resp]
            [cemerick.friend :as friend]
            [rabble.handlers.load-session :as ls]
            [rabble.handlers.session :as session]
            [rabble.resources.generate :as g])
  (:use rabble.config))

;; Require all the resources without having to type all that out
(defn as
  [sym]
  (-> sym str (clojure.string/split #"\.") last symbol))
(doseq [ns '[topic watch watched-topic post
             like stats user tag password
             credential-recovery.forgot-username
             credential-recovery.forgot-password]]
  (require [(symbol (str "rabble.resources." ns)) :as (as ns)]))

(def static-routes
  ^{:doc "Used to serve up the angular application"}
  (routes (apply compojure.core/routes
                 (map #(compojure.core/routes
                        (GET "/" [] (resp/file-response "index.html" {:root %}))
                        (GET "/" [] (resp/resource-response "index.html" {:root %})))
                      (config :html-paths)))
          
          (apply compojure.core/routes
                 (map #(compojure.core/routes
                        (compojure.route/files "/" {:root %})
                        (compojure.route/resources "/" {:root %}))
                      (config :html-paths)))))

(defroutes core-routes
  ^{:doc "Core rabble functionality with default resource options"}
  (GET "/scripts/load-session.js"
       {:keys [params] :as req}
       (ls/load-session params (friend/current-authentication req)))

  (g/resource-route "/topics"
                    topic/resource-decisions
                    :decision-options topic/default-options)
  (g/resource-route "/posts"
                    post/resource-decisions
                    :decision-options post/default-options)
  (g/resource-route "/users"
                    user/resource-decisions
                    :decision-options user/default-options)
  (g/resource-route "/watches"
                    watch/resource-decisions)
  (g/resource-route "/watched-topics"
                    watched-topic/resource-decisions
                    :decision-options watched-topic/default-options)
  (g/resource-route "/like"
                    like/resource-decisions)
  (g/resource-route "/stats"
                    stats/resource-decisions)
  (g/resource-route "/tags"
                    tag/resource-decisions
                    :decision-options tag/default-options)

  (POST "/login" {params :params} (session/create! params))
  (friend/logout (ANY "/logout" [] (ring.util.response/redirect "/"))))

(defroutes credential-routes
  ^{:doc ""}
  (ANY "/users/:id/password"
       {:keys [params] :as req}
       (password/change-password! params (friend/current-authentication req)))
  
  (g/resource-route "/credential-recovery/forgot-username"
                    forgot-username/resource-decisions)
  (g/resource-route "/credential-recovery/forgot-password"
                    forgot-username/resource-decisions
                    :entry-key :token))

(def app-routes (routes static-routes core-routes credential-routes))
