(ns rabble.email.sending.senders
  (:require com.flyingmachine.penny-black-postal
            [com.flyingmachine.penny-black.core.send :refer [defsenders]]
            [com.flyingmachine.penny-black.core.config :as email-config]
            [flyingmachine.webutils.utils :refer :all]
            [rabble.config :as rabble-config]
            [rabble.lib.html :refer :all]))

;; Topics/posts
(defsenders
  {:args [users topic]
   :user-doseq [user users]}
  {:from (email-config/config :from-address)
   :to (:user/email user)
   :body-data {:topic-title (:title topic)
               :topic-id (:id topic)
               :username (:user/username user)}}

  (send-reply-notification
   [post]
   :subject (str "[" (rabble-config/config :forum-name) "] Re: " (:title topic))
   :body-data {:content (:content post)
               :formatted-content (md-content post)})
  
  (send-new-topic-notification
   []
   :subject (str "[" (rabble-config/config :forum-name) "] " (:title topic))
   :body-data {:content (:content (:first-post topic))
               :formatted-content (md-content (:first-post topic))}))


(defsenders
  {:args [users]
   :user-doseq [user users]}
  {:from (rabble-config/config :from-address)
   :to (:user/email user)
   :body-data {:username (:user/username user)}}

  (send-forgot-username
   []
   :subject (str "Your " (rabble-config/config :forum-name) " username"))

  (send-password-reset-token
   []
   :subject (str (rabble-config/config :forum-name) " password reset")
   :body-data {:token (:user/password-reset-token user)}))
