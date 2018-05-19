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

(defn register-form [& [{:keys [username email password] :as errors}]]
  [:form {:method "post" :action "/daftar"}
   (anti-forgery-field)
   (when username
     [:div.alert.alert-warning "Invalid username"])
   (when email
     [:div.alert.alert-warning "Invalid email"])
   (when password
     [:div.alert.alert-warning "Invalid password"])
   [:div.form-group
    (label "username" "Username")
    [:input#username.form-control
     {:type "text" :name "username" :placeholder "Username" :required true}]]
   [:div.form-group
    (label "email" "Email")
    [:input#email.form-control
     {:type "email" :name "email" :placeholder "Email" :required true}]]
   [:div.form-group
    (label "password" "Password")
    [:input#password.form-control
     {:type "password" :name "password" :placeholder "Password" :required true}]]
   [:input.btn.btn-primary.btn-block {:type "submit" :value "Daftar"}]])


;; Pages

(defn index [req respond _]
  (respond
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
        (register-form)]]]])))

(defn login [req respond _]
  (respond
   (base-html
    req
    {:title "Log Masuk"}
    (login-form))))

(defn daftar [req respond _]
  (respond
   (base-html
    req
    {:title "Daftar"}
    [:section.container
     [:div.col-md-6.col-md-offset-3
      (register-form (get-in req [:flash :errors]))]])))

(defn tentang [req respond _]
  (respond
   (base-html
    req
    {:title "Tentang Kami"}
    [:section
     [:p "Now that we know who you are, I know who I am."]])))
