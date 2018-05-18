(ns binasoalan.handler
  (:require [binasoalan.views :as views]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer :all]))

(defroutes app-routes
  (GET "/" [] views/index)
  (POST "/login" [] "Logged in")
  (GET "/daftar" [] "Sign Up")
  (GET "/tentang" [] views/about)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)))
