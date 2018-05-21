(ns binasoalan.handler
  (:require [binasoalan.utils :refer [flash]]
            [binasoalan.validation :as v]
            [binasoalan.views :as views]
            [buddy.hashers :as hashers]
            [clojure.core.async :as async :refer [go chan >! <!
                                                  alts! timeout put!]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer :all]))

(def ali {:username "ali"
          :email "ali@yahoo.com"})

(defn find-user-by-username [username]
  (Thread/sleep 1000)
  (when (= username "ali")
    ali))

(defn find-user-by-email [email]
  (Thread/sleep 1000)
  (when (= email "ali@yahoo.com")
    ali))

(def user-existed-msg "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain.")
(def success-msg "Anda sudah berjaya mendaftar. Sila log masuk .")

(defn- make-flash-msg [[errors data]]
  (if errors
    [{:errors errors :data data} data]
    [nil data]))

(def validate-xform
  (comp
   (map v/validate-registration)
   (map make-flash-msg)))

(defn- validate [form]
  (let [validated (chan 1 validate-xform)]
    (put! validated form)
    validated))

(defn- find-existing-user [validated]
  (go (let [[errors user :as all] (<! validated)
            already-existed (chan)]
        (if errors
          (put! already-existed false)
          (let [by-username (async/thread (find-user-by-username (:username user)))
                by-email (async/thread (find-user-by-email (:email user)))]
            (put! already-existed (boolean (or (<! by-username) (<! by-email))))))
        (conj all already-existed))))

(defn- check-existing-user [user-check]
  (go (let [[errors user already-existed :as all] (<! user-check)]
        (cond
          errors all
          (<! already-existed) [{:message user-existed-msg} user]
          :else all))))

(defn- hash-user-password [new-user]
  (go (let [[errors user :as all] (<! new-user)]
        (if errors
          all
          [errors (async/thread (update user :password hashers/derive))]))))

(defn- persist-user [new-user]
  (go (let [[errors hashed-user :as all] (<! new-user)]
        (if errors
          all
          (do
            (println "Registered: " (<! hashed-user))
            all)))))

(defn- make-response [registered]
  (go (let [[errors _] (<! registered)]
        (if errors
          (-> (redirect "/daftar")
              (flash errors))
          (-> (redirect "/login")
              (flash success-msg))))))

(def register-xform
  (comp
   (map validate)
   (map find-existing-user)
   (map check-existing-user)
   (map hash-user-password)
   (map persist-user)
   (map make-response)))

(defn register [{:keys [params]} respond _]
  (let [form (select-keys params [:username :email :password])
        [response] (eduction register-xform [form])]
    (go (let [[res _] (alts! [response (timeout 10000)])]
          (respond res)))))

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
