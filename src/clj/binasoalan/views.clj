(ns binasoalan.views
  (:require [hiccup.element :refer [link-to]]
            [hiccup.form :refer [label text-field password-field email-field
                                 submit-button]]
            [hiccup.page :refer [html5 include-css include-js]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]))

(defn header []
  [:header
   [:a.logo {:href "#"} "Bina Soalan"]
   [:a.button {:href "/"} "Laman Utama"]
   [:a.button {:href "/tentang"} "Tentang Kami"]
   [:a.button {:href "/login"} "Log Masuk"]])

(defn nav [& [current-uri]]
  [:nav.navbar.navbar-default
   [:div.navbar-header
    [:button.navbar-toggle.collapsed {:type "button" :data-toggle "collapse"
                                      :data-target "#navbar-collapse"
                                      :aria-expanded "false"}
     [:span.sr-only "Toggle navigation"]
     [:span.icon-bar]
     [:span.icon-bar]
     [:span.icon-bar]]
    [:span.navbar-brand "Bina Soalan"]]
   [:div#navbar-collapse.collapse.navbar-collapse
    [:ul.nav.navbar-nav
     [:li {:class (if (= current-uri "/") "active" "")}
      (link-to "/" "Laman Utama")]
     [:li {:class (if (= current-uri "/tentang") "active" "")}
      (link-to "/tentang" "Tentang Kami")]
     [:li {:class (if (= current-uri "/login") "active" "")}
      (link-to "/login" "Log Masuk")]]]])

(defn base-html
  [req {:keys [title] :as attr} & contents]
  (if-not (map? attr)
    (apply base-html req {} attr contents)
    (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title (if title
                (str title " | Bina Soalan")
                "Bina Soalan")]
      (include-css
       "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
       "css/style.css")]
     [:body
      (nav (:uri req))
      contents
      (include-js
       "https://code.jquery.com/jquery-3.3.1.slim.min.js"
       "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js")])))

(defn login-form []
  [:form {:method "post" :action "/login"}
   (anti-forgery-field)
   [:div.form-group
    (label "username" "Username")
    [:input#username.form-control
     {:type "text" :name "username" :placeholder "Username" :required true}]]
   [:div.form-group
    (label "password" "Password")
    [:input#password.form-control
     {:type "password" :name "password" :placeholder "Password" :required true}]]
   [:input.btn.btn-primary.btn-block {:type "submit" :value "Login"}]])

(defn register-form [& [{:keys [errors data]}]]
  [:form {:method "post" :action "/daftar"}
   (anti-forgery-field)

   [:div.form-group {:class (if (:username errors) "has-error" "")}
    [:label.control-label {:for "username"} "Username"]
    [:input#username.form-control
     {:type "text" :name "username" :placeholder "Username"
      :value (:username data)}]
    (when (:username errors)
      [:span.help-block "Username tidak boleh kosong."])]

   [:div.form-group {:class (if (:email errors) "has-error" "")}
    [:label.control-label {:for "email"} "Email"]
    [:input#email.form-control
     {:type "text" :name "email" :placeholder "Email" :value (:email data)}]
    (when (:email errors)
      [:span.help-block "Email mesti dalam bentuk email yang sah dan tidak boleh kosong."])]

   [:div.form-group {:class (if (:password errors) "has-error" "")}
    [:label.control-label {:for "password"} "Password"]
    [:input#password.form-control
     {:type "password" :name "password" :placeholder "Password"}]
    (when (:password errors)
      [:span.help-block "Password tidak boleh kosong."])]

   [:input.btn.btn-primary.btn-block {:type "submit" :value "Daftar"}]])


;; Pages

(defn index
  ([req]
   (base-html
    req
    [:section.container
     [:div.col-md-8
      [:div.jumbotron
       [:h1 "Bina Soalan"]
       [:p "I'm bad at copywriting."]]]
     [:div.col-md-4
      [:div.panel.panel-default
       [:div.panel-body
        (register-form)]]]]))
  ([req respond _] (respond (index req))))

(defn login
  ([{{message :message} :flash :as req}]
   (base-html
    req
    {:title "Log Masuk"}
    [:section.container
     [:div.col-md-4.col-md-offset-4
      (when message
        [:div.alert.alert-success message])
      [:div.panel.panel-default
       [:div.panel-body
        (login-form)]]]]))
  ([req respond _] (respond (login req))))

(defn daftar
  ([{{message :message} :flash :as req}]
   (base-html
    req
    {:title "Daftar"}
    [:section.container
     [:div.col-md-6.col-md-offset-3
      (when message
        [:div.alert.alert-danger
         message])
      (register-form (:flash req))]]))
  ([req respond _] (respond (daftar req))))

(defn verified
  ([req]
   (base-html
    req
    {:title "Email telah disahkan"}
    [:section.container
     [:div.col-md-6.col-md-offset-3
      [:div.panel.panel-default
       [:div.panel-body.text-center
        [:p
         "Email anda telah disahkan. Anda boleh log in."]
        [:p [:a {:href "/login"} "Klik di sini untuk log in"]]]]]]))
  ([req respond _] (respond (verified req))))

(defn tentang
  ([req]
   (base-html
    req
    {:title "Tentang Kami"}
    [:section
     [:p "Now that we know who you are, I know who I am."]]))
  ([req respond _] (respond (tentang req))))
