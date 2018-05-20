(ns binasoalan.handler
  (:require [binasoalan.validation :as v]
            [binasoalan.views :as views]
            [buddy.hashers :as hashers]
            [clojure.core.async :as async :refer [go chan >! <!]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer :all]))

(defn find-user [{:keys [username email]}]
  (when (or (= username "ali")
            (= email "ali@yahoo.com"))
    {:username "ali"
     :email "ali@yahoo.com"}))

(defn register [{:keys [params]} respond _]
  (let [form (select-keys params [:username :email :password])
        validated (chan)
        user (chan)
        existing-user (chan)
        new-user (chan)
        hashed (chan)
        result (chan)]
    (go (>! validated (v/validate-registration form)))
    (go (let [[errors data :as prev] (<! validated)]
          (if errors
            (>! result (-> (redirect "/daftar")
                           (assoc :flash prev))) ; assoc previous data
            (>! user data))))
    (go (let [u          (<! user)
              found-user (async/thread (find-user u))]
          (if (<! found-user)
            (>! result "Already registered")
            (>! new-user u))))
    (go (>! hashed (update (<! new-user) :password hashers/derive)))
    (go (<! hashed)
        (>! result (-> (redirect "/login")
                       (assoc :flash "Anda sudah berjaya mendaftar. Sila log masuk ."))))
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
