(ns binasoalan.core
  (:require [binasoalan.handlers.auth :as auth]
            [binasoalan.handlers.pages :as pages]
            [binasoalan.handlers.register :as register]
            [binasoalan.security :refer [wrap-security]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.http-response :refer :all]))

(defroutes app-routes
  (GET "/" [] pages/index)
  (GET "/login" [] pages/login)
  (GET "/daftar" [] pages/daftar)
  (GET "/tentang" [] pages/tentang)
  (GET "/sahkan" [] register/verify-handler)
  (GET "/verified" [] pages/verified)
  (GET "/app" [] "Welcome to app page")
  (POST "/login" [] auth/login-handler)
  (POST "/logout" [] auth/logout-handler)
  (POST "/daftar" [] register/register-handler)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      wrap-security
      (wrap-defaults site-defaults)))
