(ns binasoalan.views
  (:require [hiccup.element :refer [link-to]]
            [hiccup.form :refer [label text-field password-field email-field
                                 submit-button]]
            [hiccup.page :refer [html5 include-css]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defmacro base-html
  [{:keys [title] :as attr} & contents]
  (if-not (map? attr)
    `(base-html {} ~attr ~@contents)
    `(html5
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
       [:title (if ~title
                 (str ~title " | Bina Soalan")
                 "Bina Soalan")]
       (include-css "css/style.css")]
      [:body ~@contents])))

(defn nav []
  [:nav
   [:li (link-to "/" "Laman Utama")]
   [:li (link-to "/tentang" "Tentang Kami")]
   [:li (link-to "/login" "Log Masuk")]])

(defn login-form []
  [:form {:method "post" :action "/login"}
   (anti-forgery-field)
   (label "username" "Username")
   (text-field "username")
   (label "password" "Password")
   (password-field "password")
   (submit-button "Login")])

(defn register-form []
  [:form {:method "post" :action "/daftar"}
   (anti-forgery-field)
   (label "username" "Username")
   (text-field "username")
   (label "email" "Email")
   (email-field "email")
   (label "password" "Password")
   (password-field "password")
   (submit-button "Daftar")])


;; Pages

(defn index [_ respond _]
  (respond
   (base-html
    [:header
     [:h1 "Bina Soalan"]]
    (nav)
    (register-form))))

(defn login [_ respond _]
  (respond
   (base-html
    {:title "Log Masuk"}
    [:header
     [:h1 "Log Masuk"]]
    (nav)
    (login-form))))

(defn tentang [_ respond _]
  (respond
   (base-html
    {:title "Tentang Kami"}
    [:header
     [:h1 "Tentang Kami"]]
    (nav)
    [:section
     [:p "Now we know who you are, I know who I am."]])))
