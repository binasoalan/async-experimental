(ns binasoalan.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :refer :all]))

(defroutes app-routes
  (GET "/" [] (redirect "/index.html"))
  (GET "/hello" [] "Hello World")
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)))
