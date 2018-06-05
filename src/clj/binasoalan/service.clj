(ns binasoalan.service
  (:require [binasoalan.handlers.auth :as auth]
            [binasoalan.utils :refer [common-interceptors]]
            [binasoalan.views :as views]
            [clojure.set :as set]
            [io.pedestal.http :as http]))


(def base-routes #{["/" :get (conj common-interceptors `views/index)]
                   ["/tentang" :get (conj common-interceptors `views/tentang)]})

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
