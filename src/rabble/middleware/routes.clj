(ns rabble.middleware.routes
  (:require compojure.route
            compojure.handler
            [ring.util.response :as resp]
            [rabble.controllers.admin.users :as ausers]
            [cemerick.friend :as friend]
            [flyingmachine.webutils.routes :refer :all]
            [rabble.resources.topic :as topic]
            [rabble.resources.generate :as g])
  (:use [compojure.core :as compojure.core :only (GET PUT POST DELETE ANY defroutes)]
        rabble.config))

(defn as
  [sym]
  (-> sym str (clojure.string/split #"\.") last symbol))
(doseq [ns '[topics watches watched-topics posts
             likes stats users session js tags
             credential-recovery.forgot-username
             credential-recovery.forgot-password]]
  (require [(symbol (str "rabble.controllers." ns)) :as (as ns)]))

(def authfn friend/current-authentication)
(defroutemacro auth-resource-routes
  :route-op authroute
  :route-args [authfn])

(defroutes rabble-routes
  (authroute GET "/scripts/load-session.js" js/load-session authfn)

  (GET "/topics" [] (g/resources :topic topic/resource-config {:topic {:list {:mapifier topic/list-topic}}}))

  ;; Serve up angular app
  (apply compojure.core/routes
         (map #(compojure.core/routes
                (GET "/" [] (resp/file-response "index.html" {:root %}))
                (GET "/" [] (resp/resource-response "index.html" {:root %})))
              (config :html-paths)))
  
  (apply compojure.core/routes
         (map #(compojure.core/routes
                (compojure.route/files "/" {:root %})
                (compojure.route/resources "/" {:root %}))
              (config :html-paths)))

  (auth-resource-routes topics :only [:show :create! :delete!])
  (auth-resource-routes watches :only [:query :create! :delete!])
  (auth-resource-routes watched-topics :only [:query])
  (auth-resource-routes posts :only [:query :create! :update! :delete!])
  (auth-resource-routes likes
                        :only [:create! :delete!]
                        :suffixes [":post-id"])
  (route GET "/tags" tags/query)
  
  ;; Users
  (authroute POST "/users" users/registration-success-response authfn)
  (route GET "/users/:id" users/show)
  (route GET "/users" users/query)
  (authroute PUT "/users/:id" users/update! authfn)

  ;; Login
  (route POST "/login" session/create!)
  (friend/logout (ANY "/logout" [] (ring.util.response/redirect "/")))

  ;; Stats
  (route GET "/stats" stats/query)

  ;; Admin users
  (authroute GET "/admin/users" ausers/query authfn))

(defroutes auth-routes
  ;; credentials
  (authroute POST "/users/:id/password" users/change-password! authfn)
  (route POST "/credential-recovery/forgot-username" forgot-username/create!)
  (route GET "/credential-recovery/forgot-password/:token" forgot-password/show)
  (route PUT "/credential-recovery/forgot-password/:token" forgot-password/update!)
  (route POST "/credential-recovery/forgot-password" forgot-password/create!))
