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


(defn build-core-routes
  "This provides an easy way to customize the options for
  resources. For more extensive customization, you'll need to write
  things out in your host project."
  [resource-options]
  (routes
   (GET "/scripts/load-session.js"
        {:keys [] :as req}
        (ls/load-session (friend/current-authentication req)))

   (g/resource-route "/topics"
                     topic/resource-decisions
                     :decision-options (:topic resource-options))
   (g/resource-route "/posts"
                     post/resource-decisions
                     :decision-options (:post resource-options))
   (g/resource-route "/users"
                     user/resource-decisions
                     :decision-options (:user resource-options))
   (g/resource-route "/watches"
                     watch/resource-decisions)
   (g/resource-route "/watched-topics"
                     watched-topic/resource-decisions
                     :decision-options (:watched-topic resource-options))
   (g/resource-route "/like"
                     like/resource-decisions)
   (g/resource-route "/stats"
                     stats/resource-decisions)
   (g/resource-route "/tags"
                     tag/resource-decisions
                     :decision-options (:tag resource-options))

   (POST "/login" {:keys [] :as req} (session/create! req))
   (friend/logout (ANY "/logout" [] (ring.util.response/redirect "/")))))

(def core-routes
  ^{:doc "Core rabble functionality with default resource options"}
  (build-core-routes {:topic topic/default-options
                      :post post/default-options
                      :user user/default-options
                      :watched-topic watched-topic/default-options
                      :tag tag/default-options}))

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

(def app-routes
  ^{:doc "All the default routes"}
  (routes core-routes credential-routes static-routes))
