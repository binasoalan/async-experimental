(ns binasoalan.handler
  (:require [binasoalan.validation :as v]
            [binasoalan.views :as views]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer :all]))

(defn register [{:keys [params]} respond _]
  (let [[errors params] (-> params
                            (select-keys [:username :email :password])
                            v/validate-registration)]
    (if errors
      (respond (-> (redirect "/daftar")
                   (assoc :flash {:errors errors})))
      (respond "Registered"))))

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
