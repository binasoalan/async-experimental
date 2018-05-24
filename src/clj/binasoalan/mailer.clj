(ns binasoalan.mailer
  (:require [binasoalan.config :as config]
            [clojure.core.async :as async :refer [go-loop >!! <! chan]]
            [environ.core :refer [env]]
            [postal.core :as postal]))

(def debug? (:debug env false))

(def user (config/decrypt (:email-username env)))
(def pass (config/decrypt (:email-password env)))

(def host {:host "smtp.gmail.com"
           :user user
           :pass pass
           :ssl true
           :port 465})

(def default-from-email "burhanclj@gmail.com")
(def default-to-email "burhanclj@gmail.com")

(def email-log (chan))

;; Email logger
(go-loop []
  (let [[from-email to-email subject status] (<! email-log)]
    (println
     "Sending email from" from-email "to" to-email
     "with subject [" subject "]... status:" status))
  (recur))

(defn send-email-verification [{:keys [email token]}]
  (let [from-email default-from-email
        to-email (if debug? default-to-email email)
        subject "Sahkan email anda"
        body (str "Klik link ini untuk mengesahkan email anda: "
                  "http://localhost:3000/sahkan?token=" token)
        status (postal/send-message host {:from from-email
                                          :to to-email
                                          :subject subject
                                          :body body})]
    (>!! email-log [from-email to-email subject status])))
