(ns binasoalan.views
  (:require [hiccup.element :refer [link-to]]
            [hiccup.form :refer [label text-field password-field submit-button]]
            [hiccup.page :refer [html5 include-css]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defmacro base-html
  [{:keys [title]} & contents]
  `(html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:title ~title]
     (include-css "css/style.css")]
    [:body ~@contents]))

(defn nav []
  [:nav
   [:li (link-to "/" "Laman Utama")]
   [:li (link-to "/about" "Tentang Kami")]])

(defn login []
  [:form {:method "post" :action "/login"}
   (anti-forgery-field)
   (label "username" "Username")
   (text-field "username")
   (label "password" "Password")
   (password-field "password")
   (submit-button "Login")])


;; Pages

(defn index [_ respond _]
  (respond
   (base-html
    {:title "Bina Soalan"}
    [:header
     [:h1 "Bina Soalan"]]
    (nav)
    (login))))

(defn about [_ respond _]
  (respond
   (base-html
    {:title "Tentang Kami - Bina Soalan"}
    [:header
     [:h1 "Tentang Kami"]]
    (nav))))
