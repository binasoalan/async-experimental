(ns binasoalan.handler
  (:require [binasoalan.validation :as v]
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

(defn register [{:keys [params]} respond _]
  (let [form (select-keys params [:username :email :password])
        validated (chan)
        user (chan)
        new-user (chan)
        hashed (chan)
        result (chan)]
    (go (>! validated (v/validate-registration form)))
    (go (let [[errors data] (<! validated)]
          (if errors
            (>! result (-> (redirect "/daftar")
                           (assoc :flash {:errors errors
                                          :data data}))) ; assoc previous data
            (>! user data))))
    (go (let [u           (<! user)
              by-username (async/thread (find-user-by-username (:username u)))
              by-email    (async/thread (find-user-by-email (:email u)))]
          (if (or (<! by-username) (<! by-email))
            (>! result (-> (redirect "/daftar")
                           (assoc :flash {:message "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain."})))
            (>! new-user u))))
    (go (>! hashed (update (<! new-user) :password hashers/derive)))
    (go (<! hashed)
        (>! result (-> (redirect "/login")
                       (assoc :flash "Anda sudah berjaya mendaftar. Sila log masuk ."))))
    (go (let [[res _] (alts! [result (timeout 10000)])]
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
