(ns binasoalan.handlers.register
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.mailer :as mailer]
            [binasoalan.utils :refer [flash]]
            [binasoalan.validation :as v]
            [binasoalan.db.users :as users]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.core.async :as async :refer [go chan >! <!
                                                  alts! timeout put!]]
            [ring.util.response :refer :all]))


;; User registration

(def user-existed-msg "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain.")
(def success-msg "Anda sudah berjaya mendaftar. Sila log masuk .")
(def failed-msg "Pendaftaran gagal. Sila cuba semula.")

(defn- make-error-msg [[errors data]]
  (if errors
    [{:errors errors :data data} data]
    [nil data]))

(def validate-xform
  (comp
   (map v/validate-registration)
   (map make-error-msg)))

(defn- validate [form]
  (let [validated (chan 1 validate-xform)]
    (put! validated form)
    validated))

(defn- find-existing-user [validated]
  (go (let [[errors user :as all] (<! validated)
            already-existed (chan)]
        (if errors
          (put! already-existed false)
          (let [by-username (async/thread
                              (users/find-user-by-username db-spec user))
                by-email (async/thread
                           (users/find-user-by-email db-spec user))]
            (put! already-existed (boolean (or (<! by-username) (<! by-email))))))
        (conj all already-existed))))

(defn- check-existing-user [user-check]
  (go (let [[errors user already-existed :as all] (<! user-check)]
        (cond
          errors all
          (<! already-existed) [{:message user-existed-msg} user]
          :else all))))

(defn- hash-user-password [new-user]
  (go (let [[errors user :as all] (<! new-user)]
        (if errors
          all
          [errors (async/thread (update user :password hashers/derive))]))))

(defn- assoc-email-verification-token [new-user]
  (go (let [[errors user :as all] (<! new-user)]
        (if errors
          all
          [errors (go (assoc (<! user) :token (-> (nonce/random-bytes 16)
                                                  (codecs/bytes->hex))))]))))

(defn- persist-user [new-user]
  (go (let [[errors prepared-user-chan :as all] (<! new-user)]
        (if errors
          all
          (let [prepared-user (<! prepared-user-chan)
                row-count (async/thread
                            (users/register-user db-spec prepared-user))]
            [errors prepared-user row-count])))))

(defn- send-email-verification [persisted-user-chan]
  (go (let [[errors persisted-user row-count :as all] (<! persisted-user-chan)]
        (cond
          errors all
          (zero? (<! row-count)) [{:message failed-msg} persisted-user]
          :else (do
                  (mailer/send-email-verification persisted-user)
                  [errors persisted-user])))))

(defn- make-register-response [registered]
  (go (let [[errors _] (<! registered)]
        (if errors
          (-> (redirect "/daftar")
              (flash errors))
          (-> (redirect "/login")
              (flash success-msg))))))

(def x-prepare-user
  (comp
   (map hash-user-password)
   (map assoc-email-verification-token)))

(def x-register
  (comp
   (map validate)
   (map find-existing-user)
   (map check-existing-user)
   x-prepare-user
   (map persist-user)
   (map send-email-verification)
   (map make-register-response)))

(defn register [{:keys [params]} respond _]
  (let [form (select-keys params [:username :email :password])
        [response] (eduction x-register [form])]
    (go (let [[res _] (alts! [response (timeout 10000)])]
          (respond res)))))


;; Email verification

(def no-token-msg "No token supplied.")

(defn- get-token [params]
  (go (let [{:keys [token]} params]
        (if token
          [nil token]
          [{:message no-token-msg} nil]))))

(defn- verify-email [token-chan]
  (go (let [[errors token :as all] (<! token-chan)]
        (if errors
          all
          (conj all (async/thread (users/verify-user db-spec token)))))))

(defn- make-verify-response [result-chan]
  (go (let [[errors _ rows-count] (<! result-chan)]
        (cond
          errors (redirect "/login")
          (zero? (<! rows-count)) (redirect "/login")
          :else "Verified"))))

(def x-verify
  (comp
   (map get-token)
   (map verify-email)
   (map make-verify-response)))

(defn verify [{:keys [params]} respond _]
  (let [[response] (eduction x-verify [params])]
    (go (let [[res _] (alts! [response (timeout 10000)])]
          (respond res)))))
