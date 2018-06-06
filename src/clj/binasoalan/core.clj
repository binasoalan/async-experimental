(ns binasoalan.core
  (:require [binasoalan.handlers.auth :as auth]
            [binasoalan.handlers.register :as register]
            [binasoalan.security :refer [wrap-security]]
            [binasoalan.views :as views]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer :all]))

(defroutes app-routes
  (GET "/" [] views/index)
  (GET "/login" [] views/login)
  (GET "/daftar" [] views/daftar)
  (GET "/tentang" [] views/tentang)
  (GET "/sahkan" [] register/verify-handler)
  (GET "/verified" [] views/verified)
  (GET "/app" [] "Welcome to app page")
  (POST "/login" [] auth/login-handler)
  (POST "/logout" [] auth/logout-handler)
  (POST "/daftar" [] register/register-handler)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-security
      (wrap-defaults site-defaults)))
