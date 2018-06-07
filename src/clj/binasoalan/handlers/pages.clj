(ns binasoalan.handlers.pages
  (:require [binasoalan.utils.enlive :refer [defcached]]
            [binasoalan.views :refer :all]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Index page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcached index-fragment [uri]
  (base {:uri uri
         :title "Bina Soalan"
         :content (index-content)}))

(defn index
  ([{:keys [uri]}]     (embed-csrf-token (index-fragment uri)))
  ([request respond _] (respond (index request))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Login page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcached login-fragment [uri errors]
  (base {:uri uri
         :title "Log Masuk | Bina Soalan"
         :content (login-content errors)}))

(defn login
  ([request]              (login request {}))
  ([{:keys [uri]} errors] (embed-csrf-token (login-fragment uri errors)))
  ([request respond _]    (respond (login request))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Daftar page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcached daftar-fragment [uri errors]
  (base {:uri uri
         :title "Daftar | Bina Soalan"
         :content (daftar-content errors)}))

(defn daftar
  ([request]              (daftar request {}))
  ([{:keys [uri]} errors] (embed-csrf-token (daftar-fragment uri errors)))
  ([request respond _]    (respond (daftar request))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Verified page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcached verified-fragment [uri]
  (base {:uri uri
         :title "Email telah disahkan | Bina Soalan"
         :content (verified-content)}))

(defn verified
  ([{:keys [uri]}]     (verified-fragment uri))
  ([request respond _] (respond (verified request))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tentang page
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defcached tentang-fragment [uri]
  (base {:uri uri
         :title "Tentang Kami | Bina Soalan"
         :content (tentang-content)}))

(defn tentang
  ([{:keys [uri]}]     (tentang-fragment uri))
  ([request respond _] (respond (tentang request))))
