(ns binasoalan.handlers.auth
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.db.users :as users]
            [binasoalan.utils :refer [flash split-if-error common-interceptors]]
            [binasoalan.validation :as v]
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
            (let [params (get-in context [:request :params])]
              (if-let [errors (-> params v/validate-login first)]
                (assoc context :response invalid-input-response)
                context)))})

(def lookup-user
  {:name ::lookup-user
   :enter (fn [context]
            (let [params (get-in context [:request :params])]
              (async/thread
                (if-let [user (users/find-user-by-username db-spec params)]
                  (assoc context ::found-user user)
                  (assoc context :response wrong-credentials-response)))))})

(def validate-password
  {:name ::validate-password
   :enter (fn [context]
            (let [input-password (get-in context [:request :params :password])
                  user           (::found-user context)
                  match?         (hashers/check input-password (:password user))]
              (if match?
                context
                (assoc context :response wrong-credentials-response))))})

(defn authenticate
  [request]
  (let [remember?              (get-in request [:params :remember])
        session                (:session request)
        identity               (get-in request [:params :username])
        updated-session        (assoc session :identity identity)
        authenticated-response (-> (resp/ok "Logged in")
                                   (assoc :session updated-session))]
    (if remember?
      (assoc authenticated-response :session-cookie-attrs {:max-age 31557600})
      authenticated-response)))


(def login-interceptors (conj common-interceptors
                              `validate-input
                              `lookup-user
                              `validate-password
                              `authenticate))


(defn logout
  [request]
  (-> (resp/found "/")
      (assoc :session {})))

(def logout-interceptors (conj common-interceptors `logout))


(def routes #{["/login" :post login-interceptors]
              ["/logout" :post logout-interceptors]})

;; (defn- validate-password [password user]
;;   (if (and (seq user)
;;            (hashers/check password (:password user)))
;;     [nil user]
;;     [{:error "wrong username/password"} user]))

;; (defn- lookup-user
;;   "Lookup user from database and then compare the password. The result is a vector
;;   of which the first value is the error hashmap, and the second value is the
;;   user hashmap used previously to check. The result is pipelined to out-ch
;;   channel supplied in the second parameter."
;;   [[_ user] out-ch]
;;   (let [password (:password user)]
;;     (->> (users/find-user-by-username db-spec user)
;;          (merge {})
;;          (async/thread)
;;          (async/pipeline 1 out-ch (map (partial validate-password password))))))

;; (defn- authenticate
;;   "Authenticate request by adding :identity key to session with username."
;;   [req [_ user]]
;;   (let [remember?          (get-in req [:params :remember])
;;         session            (:session req)
;;         identity           (select-keys user [:username])
;;         updated-session    (assoc session :identity identity)
;;         authenticated-resp (-> (response "Logged in")
;;                                (content-type "text/html")
;;                                (assoc :session updated-session))]
;;     (if remember?
;;       (assoc authenticated-resp :session-cookie-attrs {:max-age 31557600})
;;       authenticated-resp)))

;; (defn login-handler
;;   "Handler for user login."
;;   [{:keys [params] :as req} respond _]
;;   (let [input-ch                (chan 1 (map v/validate-login))
;;         [invalid-ch valid-ch]   (split-if-error input-ch)
;;         lookup-ch               (chan)
;;         [not-found-ch found-ch] (split-if-error lookup-ch)
;;         auth-ch                 (chan)]
;;     (->> valid-ch
;;          (async/pipeline-async 1 lookup-ch lookup-user))
;;     (->> found-ch
;;          (async/pipeline 1 auth-ch (map (partial authenticate req))))

;;     (put! input-ch params)

;;     (go
;;       (respond
;;        (alt!
;;          invalid-ch
;;          ([] (-> (redirect "/login")
;;                  (flash {:message "invalid input"})))

;;          not-found-ch
;;          ([] (-> (redirect "/login")
;;                  (flash {:message "wrong username/password"})))

;;          auth-ch
;;          ([result] result)))
;;       (close! input-ch))))





;; (defn logout-handler
;;   "Handler for user logout."
;;   [_ respond _]
;;   (respond (-> (redirect "/")
;;                (assoc :session {}))))
