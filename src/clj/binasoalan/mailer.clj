(ns binasoalan.mailer
  (:require [clojure.core.async :as async :refer [go-loop >!! <! chan]]
            [postal.core :as postal]))

(def host {:host "smtp.gmail.com"
           :user "burhanloey@gmail.com"
           :pass ""
           :ssl true
           :port 465})

(def default-from-email "burhanclj@gmail.com")

(def email-log (chan))

;; Email logger
(go-loop []
  (let [[from-email to-email subject status] (<! email-log)]
    (println
     "Sending email from" from-email "to" to-email
     "with subject [" subject "]... status:" status))
  (recur))

(defn send-email-verification [{:keys [email token]}]
  (async/thread
    (let [from-email default-from-email
          to-email email
          subject "Sahkan email anda"
          body (str "Klik link ini untuk mengesahkan email anda: "
                    "http://localhost:3000/sahkan?token=" token)
          status (postal/send-message host {:from from-email
                                            :to to-email
                                            :subject subject
                                            :body body})]
      (>!! email-log [from-email to-email subject status]))))
