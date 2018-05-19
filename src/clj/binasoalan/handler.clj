(ns binasoalan.handler
  (:require [binasoalan.views :as views]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer :all]))

(defroutes app-routes
  (GET "/" [] views/index)
  (GET "/login" [] views/login)
  (POST "/login" [] "Logged in")
  (POST "/daftar" [] "Registered")
  (GET "/tentang" [] views/tentang)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)))
