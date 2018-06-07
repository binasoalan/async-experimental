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
