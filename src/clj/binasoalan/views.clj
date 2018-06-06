(ns binasoalan.views
  (:require [net.cgrand.enlive-html :as html]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(html/defsnippet nav "templates/nav.html" [:nav]
  [uri]
  [:ul.navbar-nav [:a (html/attr= :href uri)]] (html/add-class "active"))

(html/defsnippet login-form "templates/login_form.html" [:form]
  []
  [:#anti-forgery-token] (html/set-attr :value *anti-forgery-token*))

(defn only-when [p transform-fn]
  (if p
    transform-fn
    identity))

(defn show-when [p]
  (when p
    identity))

(html/defsnippet register-form "templates/register_form.html" [:form]
  [& [{:keys [errors data]}]]
  [:#anti-forgery-token] (html/set-attr :value *anti-forgery-token*)
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

(html/defsnippet index-content "templates/index_content.html" [:section]
  []
  [:#register-form] (html/content (register-form)))

(html/defsnippet login-content "templates/login_content.html" [:section]
  [& [{:keys [message] :as errors}]]
  [:#login-form] (html/substitute (login-form))
  [:#message]    (when message
                   (html/content message)))

(html/defsnippet daftar-content "templates/daftar_content.html" [:section]
  [& [{:keys [message] :as errors}]]
  [:#register-form] (html/substitute (register-form errors))
  [:#message]       (when message
                      (html/content message)))

(html/defsnippet verified-content "templates/verified_content.html" [:section]
  [])

(html/defsnippet tentang-content "templates/tentang_content.html" [:section]
  [])


;; Pages


(defn index
  ([request]           (base {:uri (:uri request)
                              :title "Bina Soalan"
                              :content (index-content)}))
  ([request respond _] (respond (index request))))

(defn login
  ([request]           (login request {}))
  ([request errors]    (base {:uri (:uri request)
                              :title "Log Masuk | Bina Soalan"
                              :content (login-content errors)}))
  ([request respond _] (respond (login request))))

(defn daftar
  ([request]           (daftar request {}))
  ([request errors]    (base {:uri (:uri request)
                              :title "Daftar | Bina Soalan"
                              :content (daftar-content errors)}))
  ([request respond _] (respond (daftar request))))

(defn verified
  ([request]           (base {:uri (:uri request)
                              :title "Email telah disahkan | Bina Soalan"
                              :content (verified-content)}))
  ([request respond _] (respond (verified request))))

(defn tentang
  ([request]           (base {:uri (:uri request)
                              :title "Tentang Kami | Bina Soalan"
                              :content (tentang-content)}))
  ([request respond _] (respond (tentang request))))
