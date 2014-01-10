(ns rabble.email.sending.senders
  (:require com.flyingmachine.penny-black-postal
            [com.flyingmachine.penny-black.core.send :refer [defsenders]]
            [com.flyingmachine.penny-black.core.config :as email-config]
            [flyingmachine.webutils.utils :refer :all]
            [rabble.config :as rabble-config]
            [rabble.lib.html :refer :all]))

(def forum-name (rabble-config/config :forum-name))
(def url-base (rabble-config/config :url-base))

;; Topics/posts
(defsenders
  {:args [users topic author]
   :user-doseq [user users]}
  {:from-address (email-config/config :from-address)
   :to (:user/email user)
   :body-data {:topic-title (:title topic)
               :topic-id (:id topic)
               :author author
               :username (:user/username user)
               :forum-name forum-name
               :url-base url-base}}

  (send-reply-notification
   [post]
   :subject (str "[" forum-name "] Re: " (:title topic))
   :body-data {:content (:content post)
               :formatted-content (md-content post)})
  
  (send-new-topic-notification
   []
   :subject (str "[" forum-name "] " (:title topic))
   :body-data {:content (:content (:first-post topic))
               :formatted-content (md-content (:first-post topic))}))


(defsenders
  {:args [users]
   :user-doseq [user users]}
  {:from-address (email-config/config :from-address)
   :to (:user/email user)
   :body-data {:username (:user/username user)
               :forum-name forum-name
               :url-base url-base}}

  (send-forgot-username
   []
   :subject (str "Your " forum-name " username"))

  (send-password-reset-token
   []
   :subject (str forum-name " password reset")
   :body-data {:token (:user/password-reset-token user)}))
