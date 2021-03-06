(ns rabble.test.resource-helpers
  (:require [com.flyingmachine.datomic-junk :as dj]
            [rabble.middleware.auth :as am]
            [clojure.data.json :as json]
            [compojure.core :as compojure]
            [rabble.resources.shared :as shared]
            [rabble.resources.generate :as g]
            [rabble.test.db-helpers :refer :all])
  (:use midje.sweet
        flyingmachine.webutils.utils
        rabble.paths
        [rabble.middleware.logging :only (wrap-exception)]
        ring.middleware.params
        ring.middleware.keyword-params
        ring.middleware.nested-params
        ring.middleware.session
        ring.middleware.format
        ring.middleware.anti-forgery
        [ring.mock.request :only [request header content-type]]))

(defn wrap-auth-token
  [f]
  (fn [request]
    (f (assoc request :session {:ring.middleware.anti-forgery/anti-forgery-token"foo"}))))

(defn wrap
  [to-wrap]
  (-> to-wrap
      wrap-anti-forgery
      wrap-auth-token
      (wrap-restful-format :formats [:json-kw])
      wrap-exception
      wrap-keyword-params
      wrap-nested-params
      wrap-params))

(defn test-route
  [route]
  (wrap route))

(defnpd resource-app
  [path resource-decision-generator [decision-options {}] [decision-defaults shared/default-decisions] [entry-key ":id"]]
  (let [resources (g/generate-resources resource-decision-generator
                                        decision-options
                                        decision-defaults
                                        {})]
    (test-route (compojure/routes
                 (compojure/ANY path [] (:collection resources))
                 (compojure/ANY (str path "/" entry-key) [] (:entry resources))))))

(defn authenticated
  [request auth]
  (if auth
    (merge request {:authentications {:test auth}
                    :current :test})
    request))

(defnpd req
  [method path [params nil] [auth nil]]
  (-> (request method path params)
      (header "x-csrf-token" "foo")
      (content-type "application/json")
      (authenticated auth)))

(defnpd jreq
  [method path [params nil] [auth nil]]
  (req method path (json/write-str params) auth))

(defn data
  [{:keys [body]}]
  (if (empty? body)
    nil
    (json/read-str body)))

(defnpd app-req
  [app method url [params nil] [auth nil]]
  ((app) (jreq method url params auth)))

(defnpd app-data
  [app method url [params nil] [auth nil]]
  (data (app-req app method url params auth)))

(defn content-id
  ([content-attribute] (content-id content-attribute "flyingmachine"))
  ([content-attribute author-username]
     (:db/id (dj/one content-attribute [:content/author (:id (auth author-username))]))))

(def topic-id (partial content-id :topic/title))
(def post-id (partial content-id :post/content))
