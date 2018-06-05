(ns binasoalan.handlers.auth
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.db.users :as users]
            [binasoalan.utils :refer [flash split-if-error common-interceptors]]
            [binasoalan.validation :as v]
            [binasoalan.views :as views]
            [buddy.hashers :as hashers]
            [clojure.core.async :as async]
            [ring.util.http-response :as resp]))

(def invalid-input-response
  (-> (resp/found "/login")
      (flash {:message "invalid input"})))

(def wrong-credentials-response
  (-> (resp/found "/login")
      (flash {:message "wrong username/password"})))

(def validate-input
  {:name ::validate-input
   :enter (fn [context]
            (let [params (get-in context [:request :form-params])]
              (if-let [errors (-> params v/validate-login first)]
                (assoc context :response invalid-input-response)
                context)))})

(def lookup-user
  {:name ::lookup-user
   :enter (fn [context]
            (let [params (get-in context [:request :form-params])]
              (async/thread
                (if-let [user (users/find-user-by-username db-spec params)]
                  (assoc context ::found-user user)
                  (assoc context :response wrong-credentials-response)))))})

(def validate-password
  {:name ::validate-password
   :enter (fn [context]
            (let [input-password (get-in context [:request :form-params :password])
                  user           (::found-user context)
                  match?         (hashers/check input-password (:password user))]
              (if match?
                context
                (assoc context :response wrong-credentials-response))))})

(def login-interceptors (conj common-interceptors
                              `validate-input
                              `lookup-user
                              `validate-password))

(defn authenticate
  [request]
  (let [remember?              (get-in request [:form-params :remember])
        session                (:session request)
        identity               (get-in request [:form-params :username])
        updated-session        (assoc session :identity identity)
        authenticated-response (-> (resp/ok "Logged in")
                                   (assoc :session updated-session))]
    (if remember?
      (assoc authenticated-response :session-cookie-attrs {:max-age 31557600})
      authenticated-response)))


(defn logout
  [request]
  (-> (resp/found "/")
      (assoc :session {})))


(def routes #{["/login" :get (conj common-interceptors `views/login)]
              ["/login" :post (conj login-interceptors `authenticate)]
              ["/logout" :post (conj common-interceptors `logout)]})
