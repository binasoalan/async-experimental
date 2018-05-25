(ns binasoalan.handlers.register
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.mailer :as mailer]
            [binasoalan.utils :refer [flash]]
            [binasoalan.validation :as v]
            [binasoalan.db.users :as users]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.core.async :as async :refer [go chan >! <! <!! >!!
                                                  alts! timeout put! close!
                                                  thread take!]]
            [ring.util.response :refer :all]))

(def msg {:user-existed "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain."
          :success "Anda sudah berjaya mendaftar. Sila check email untuk mengesahkan email anda."
          :failed "Pendaftaran gagal. Sila cuba semula."})


;; User registration

(defn- validate-form [[response-chan form]]
  (let [form-chan (chan)]
    (go
      (let [[errors _] (v/validate-registration form)
            valid? (not errors)]
        (if valid?
          (>! form-chan form)
          (do
            (close! form-chan)
            (>! response-chan (-> (redirect "/daftar")
                                  (flash {:errors errors :data form})))))))
    [response-chan form-chan]))

(defn- check-existing-user [[response-chan form-chan]]
  (let [user-chan (chan)]
    (go
      (if-let [form (<! form-chan)]
        (let [username-existed (thread (users/find-user-by-username db-spec form))
              email-existed (thread (users/find-user-by-email db-spec form))
              available? (not (or (<! username-existed)
                                  (<! email-existed)))]
          (if available?
            (>! user-chan form)
            (do
              (close! user-chan)
              (>! response-chan (-> (redirect "/daftar")
                                    (flash {:message (:user-existed msg)}))))))
        (close! user-chan)))
    [response-chan user-chan]))

(defn- persist-user [[response-chan user-chan]]
  (go
    (when-let [user (<! user-chan)]
      (let [email-verification-token (codecs/bytes->hex (nonce/random-bytes 16))
            prepared-user (-> user
                              (update :password hashers/derive)
                              (assoc :token email-verification-token))
            success? (->> (thread (users/register-user db-spec prepared-user))
                          <!
                          pos?)]
        (if success?
          (do
            (future (mailer/send-email-verification user))
            (>! response-chan (-> (redirect "/login")
                                  (flash {:message (:success msg)}))))
          (>! response-chan (-> (redirect "/daftar")
                                (flash {:message (:failed msg)}))))))))

(defn register-handler [{:keys [params] :as req} respond _]
  (let [response-chan (chan)
        form (select-keys params [:username :email :password])]
    (go
      (->> [response-chan form]
           validate-form
           check-existing-user
           persist-user))
    (go
      (respond (<! response-chan))
      (close! response-chan))))


;; Email verification

(defn- check-token [[response-chan token]]
  (let [token-chan (chan)]
    (go
      (let [valid? (->> (thread (users/find-token db-spec {:token token}))
                        <!
                        :token)]
        (if valid?
          (>! token-chan token)
          (do
            (close! token-chan)
            (>! response-chan (redirect "/login"))))))
    [response-chan token-chan]))

(defn- verify [[response-chan token-chan]]
  (go
    (when-let [token (<! token-chan)]
      (let [success? (->> (thread (users/verify-user db-spec {:token token}))
                          <!
                          pos?)]
        (if success?
          (>! response-chan (redirect "/verified"))
          (>! response-chan (redirect "/login")))))))

(defn verify-handler [{{token :token} :params :as req} respond _]
  (if (nil? token)
    (redirect "/login")
    (let [response-chan (chan)]
      (go
        (->> [response-chan token]
             check-token
             verify))
      (go
        (respond (<! response-chan))
        (close! response-chan)))))
