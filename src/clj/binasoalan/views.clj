(ns binasoalan.views
  (:require [binasoalan.utils.enlive :refer :all]
            [net.cgrand.enlive-html :as html]))

(html/deftemplate base "templates/base.html"
  [{:keys [uri title content]}]
  [:head :title]            (html/content title)
  [:header :ul.navbar-nav
   [:li (has-link-to uri)]] (html/add-class "active")
  [:#content]               (html/substitute content))

(html/defsnippets "templates/forms.html"
  [login-form [:#login-form] []]
  [register-form [:#register-form]
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
   [:#password-error]      (show-when (:password errors))])

(html/defsnippets "templates/contents.html"
  [index-content [:#index-content] []
   [:#register-form] (html/content (register-form))]

  [login-content [:#login-content]
   [& [{:keys [message] :as errors}]]
   [:#login-form] (html/substitute (login-form))
   [:#message]    (when message
                    (html/content message))]

  [daftar-content [:#daftar-content]
   [& [{:keys [message] :as errors}]]
   [:#register-form] (html/substitute (register-form errors))
   [:#message]       (when message
                       (html/content message))]

  [verified-content [:#verified-content] []]
  [tentang-content [:#tentang-content] []])


;; Pages

(defcached index-fragment [uri]
  (base {:uri uri
         :title "Bina Soalan"
         :content (index-content)}))

(defn index
  ([{:keys [uri]}]     (embed-csrf-token (index-fragment uri)))
  ([request respond _] (respond (index request))))


(defcached login-fragment [uri errors]
  (base {:uri uri
         :title "Log Masuk | Bina Soalan"
         :content (login-content errors)}))

(defn login
  ([request]              (login request {}))
  ([{:keys [uri]} errors] (embed-csrf-token (login-fragment uri errors)))
  ([request respond _]    (respond (login request))))


(defcached daftar-fragment [uri errors]
  (base {:uri uri
         :title "Daftar | Bina Soalan"
         :content (daftar-content errors)}))

(defn daftar
  ([request]              (daftar request {}))
  ([{:keys [uri]} errors] (embed-csrf-token (daftar-fragment uri errors)))
  ([request respond _]    (respond (daftar request))))


(defcached verified-fragment [uri]
  (base {:uri uri
         :title "Email telah disahkan | Bina Soalan"
         :content (verified-content)}))

(defn verified
  ([{:keys [uri]}]     (verified-fragment uri))
  ([request respond _] (respond (verified request))))


(defcached tentang-fragment [uri]
  (base {:uri uri
         :title "Tentang Kami | Bina Soalan"
         :content (tentang-content)}))

(defn tentang
  ([{:keys [uri]}]     (tentang-fragment uri))
  ([request respond _] (respond (tentang request))))
