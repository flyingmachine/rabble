(ns rabble.resources.post-test
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.resources.post :as post]
            [rabble.resources.shared :as shared]
            [rabble.db.validations :as validations]
            [rabble.ring-app :as ra])
  (:use midje.sweet
        rabble.paths
        rabble.test.resource-helpers
        rabble.test.db-helpers))

(setup-db-background)

(def post-options
  {:list {:mapifier post/post}
   :create {:validation (:create validations/post)
            :after-create (fn [ctx record])}
   :update {:validation (:update validations/post)}
   :show {:mapifier post/post}})

(defn test-app
  []
  (resource-app "/posts" post/resource-decisions post-options))

(facts "creation"
  (fact "a valid post with a valid user results in success"
    (app-data test-app :post "/posts" {:content "test" :topic-id (topic-id)} (auth))
    => (contains {"topic-id" (topic-id)}))
  (fact "creating a valid post without a user results in failure"
    (app-req test-app :post "/posts" {:content "test" :topic-id (topic-id)} nil)
    => (contains {:status 401}))
  (fact "creating a post without content returns errors"
    (app-req test-app :post "/posts" {:topic-id (topic-id)} (auth))
    => (contains {:status 400}))
  (fact "creating a post without a topic id returns errors"
    (app-req test-app :post "/posts" {:content "test"} (auth))
    => (contains {:status 400})))

(facts "posts can only be updated by their authors or moderators"
  (fact "updating a post as the author results in success"
    (let [username "flyingmachine"
          post-id (post-id username)]
      (app-data test-app :put (post-path post-id) {:content "new content"} (auth username))
      => (contains {"id" post-id
                    "content" "new content"}))))

(fact "after a succesful deletion the deleted attribute is true"
  (do
    (app-req test-app :delete (post-path (post-id "flyingmachine")) nil (auth "flyingmachine"))
    (into {} (dj/ent (post-id))))
    => (contains {:content/deleted true}))

(facts "posts can only be deleted by their authors or moderators"
  (fact "deleting a post as the author results in success"
    (app-req test-app :delete (post-path (post-id "flyingmachine")) nil (auth "flyingmachine"))
    => (contains {:status 204}))
  (fact "deleting a post as a moderator results in success"
    (app-req test-app :delete (post-path (post-id "joebob")) nil (auth "flyingmachine"))
    => (contains {:status 204}))
  (fact "deleting a post as not the author results in failure"
    (app-req test-app :delete (post-path (post-id "flyingmachine")) nil (auth "joebob"))
    => (contains {:status 401})))

(fact "you can't update a deleted post"
  (let [username "flyingmachine"
        auth (auth username)
        post-id (post-id username)]
    (do (app-req test-app :delete (post-path post-id) nil auth)
        (app-req test-app :put (post-path post-id) {:content "new content"} auth))
    => (contains {:status 401})))
