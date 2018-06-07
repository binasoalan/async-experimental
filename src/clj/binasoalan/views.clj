(ns binasoalan.views
  (:require [binasoalan.utils.enlive :refer [only-when show-when embed-csrf-token
                                             define-fragment]]
            [net.cgrand.enlive-html :as html]))

(html/defsnippet nav "templates/nav.html" [:nav]
  [uri]
  [:ul.navbar-nav [:a (html/attr= :href uri)]] (html/add-class "active"))

(html/defsnippet login-form "templates/login_form.html" [:form]
  [])

(html/defsnippet register-form "templates/register_form.html" [:form]
  [& [{:keys [errors data]}]]
  [:#username-form-group] (->> (html/add-class "has-error")
                               (only-when (:username errors)))
  [:#email-form-group]    (->> (html/add-class "has-error")
                               (only-when (:email errors)))
  [:#password-form-group] (->> (html/add-class "has-error")
                               (only-when (:password errors)))
  [:#username]            (->> (html/set-attr :value (:username data))
                               (only-when (:username data)))
  [:#email]               (->> (html/set-attr :value (:email data))
                               (only-when (:email data)))
  [:#username-error]      (show-when (:username errors))
  [:#email-error]         (show-when (:email errors))
  [:#password-error]      (show-when (:password errors)))

(html/deftemplate base "templates/base.html"
  [{:keys [uri title content]}]
  [:head :title] (html/content title)
  [:header]      (html/content (nav uri))
  [:#content]    (html/substitute content))

(html/defsnippets "templates/contents.html"
  [index-content [:#index-content] []
   [:#register-form] (html/content (register-form))]

  [login-content [:#login-content] [& [{:keys [message] :as errors}]]
   [:#login-form] (html/substitute (login-form))
   [:#message]    (when message
                    (html/content message))]

  [daftar-content [:#daftar-content] [& [{:keys [message] :as errors}]]
   [:#register-form] (html/substitute (register-form errors))
   [:#message]       (when message
                       (html/content message))]

  [verified-content [:#verified-content] []]
  [tentang-content [:#tentang-content] []])


;; Pages

(define-fragment index-fragment [uri]
  (base {:uri uri
         :title "Bina Soalan"
         :content (index-content)}))

(defn index
  ([{:keys [uri]}]     (embed-csrf-token (index-fragment uri)))
  ([request respond _] (respond (index request))))


(define-fragment login-fragment [uri errors]
  (base {:uri uri
         :title "Log Masuk | Bina Soalan"
         :content (login-content errors)}))

(defn login
  ([request]              (login request {}))
  ([{:keys [uri]} errors] (embed-csrf-token (login-fragment uri errors)))
  ([request respond _]    (respond (login request))))


(define-fragment daftar-fragment [uri errors]
  (base {:uri uri
         :title "Daftar | Bina Soalan"
         :content (daftar-content errors)}))

(defn daftar
  ([request]              (daftar request {}))
  ([{:keys [uri]} errors] (embed-csrf-token (daftar-fragment uri errors)))
  ([request respond _]    (respond (daftar request))))


(define-fragment verified-fragment [uri]
  (base {:uri uri
         :title "Email telah disahkan | Bina Soalan"
         :content (verified-content)}))

(defn verified
  ([{:keys [uri]}]     (verified-fragment uri))
  ([request respond _] (respond (verified request))))


(define-fragment tentang-fragment [uri]
  (base {:uri uri
         :title "Tentang Kami | Bina Soalan"
         :content (tentang-content)}))

(defn tentang
  ([{:keys [uri]}]     (tentang-fragment uri))
  ([request respond _] (respond (tentang request))))
