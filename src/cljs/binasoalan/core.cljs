(ns binasoalan.core
  (:require [binasoalan.config :as config]
            [binasoalan.views :as views]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (r/render [views/app] (js/document.getElementById "app")))

(defn ^:export init []
  (dev-setup)
  (mount-root))
