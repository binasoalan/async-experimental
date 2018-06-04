(ns binasoalan.service
  (:require [binasoalan.handlers.auth :as auth]
            [binasoalan.utils :refer [common-interceptors]]
            [binasoalan.views :as views]
            [clojure.set :as set]
            [io.pedestal.http :as http]
            [io.pedestal.http.csrf :as csrf]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.http-response :as resp]))

(defn about-page
  [request]
  (resp/ok (format "Clojure %s - served from %s"
                   (clojure-version)
                   (route/url-for ::about-page))))

(defn home-page
  [request]
  (resp/ok "Hello World!"))


(def base-routes #{["/" :get (conj common-interceptors `views/index)]
                   ["/daftar" :post `home-page]
                   ["/about" :get (conj common-interceptors `about-page)]})

(def routes (set/union base-routes
                       auth/routes))


(def service {:env :prod
              ::http/routes routes
              ::http/enable-csrf {}
              ::http/enable-session {}
              ::http/resource-path "/public"
              ::http/type :jetty
              ::http/port 3000
              ::http/container-options {:h2c? true
                                        :h2? false
                                        :ssl? false}})
