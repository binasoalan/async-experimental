(ns binasoalan.handler
  (:require [binasoalan.utils :refer [flash]]
            [binasoalan.validation :as v]
            [binasoalan.views :as views]
            [buddy.hashers :as hashers]
            [clojure.core.async :as async :refer [go chan >! <! alts! timeout]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer :all]))

(def ali {:username "ali"
          :email "ali@yahoo.com"})

(defn find-user-by-username [username]
  (when (= username "ali")
    ali))

(defn find-user-by-email [email]
  (when (= email "ali@yahoo.com")
    ali))

(def user-existed-msg "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain.")
(def success-msg "Anda sudah berjaya mendaftar. Sila log masuk .")

(defn- make-flash-msg [[errors data]]
  (if errors
    [{:errors errors :data data} data]
    [nil data]))

(defn- hash-user-password [[errors user :as all]]
  (if errors
    all
    [errors (update user :password hashers/derive)]))

(defn- make-response [[errors _]]
  (if errors
    (-> (redirect "/daftar")
        (flash errors))
    (-> (redirect "/login")
        (flash success-msg))))

(def validate-xform
  (comp
   (map v/validate-registration)
   (map make-flash-msg)))

(def register-xform
  (comp
   (map hash-user-password)
   ;; TODO: Add one more function here in the middle to persist user
   (map make-response)))

(defn register [{:keys [params]} respond _]
  (let [validated (chan 1 validate-xform)
        result (chan 1 register-xform)]
    (go (>! validated (select-keys params [:username :email :password])))
    (go (let [[errors user :as all] (<! validated)]
          (cond
            ;; If already has errors, skip checking for existing user.
            errors (>! result all)
            ;; Check for existing user with same username or email.
            (or (<! (async/thread (find-user-by-username (:username user))))
                (<! (async/thread (find-user-by-email (:email user)))))
            (>! result [{:message user-existed-msg} user])
            ;; Return same data if there is no problem.
            :else (>! result all))))
    (go (respond (<! result)))))

(defroutes app-routes
  (GET "/" [] views/index)
  (GET "/login" [] views/login)
  (GET "/daftar" [] views/daftar)
  (GET "/tentang" [] views/tentang)
  (POST "/login" [] "Logged in")
  (POST "/daftar" [] register)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)))
