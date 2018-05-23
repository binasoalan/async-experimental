(ns binasoalan.handlers.register
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.mailer :as mailer]
            [binasoalan.utils :refer [flash]]
            [binasoalan.validation :as v]
            [binasoalan.db.users :as users]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.core.async :as async :refer [go chan mult tap >! <! <!! >!!
                                                  alts! timeout put! go-loop
                                                  pub sub]]
            [ring.util.response :refer :all]))


;; User registration

(def user-existed-msg "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain.")
(def success-msg "Anda sudah berjaya mendaftar. Sila log masuk .")
(def failed-msg "Pendaftaran gagal. Sila cuba semula.")


(def pub-chan (chan))
(def publication (pub pub-chan :msg-type))

(def response-chan (chan))
(sub publication :response response-chan)

(let [raw-input-chan (chan)]
  (sub publication :raw-input raw-input-chan)
  (go-loop []
    (let [{:keys [form]} (<! raw-input-chan)
          [errors valid-form] (v/validate-registration form)]
      (if errors
        (>! pub-chan {:msg-type :response
                      :response (-> (redirect "/daftar")
                                    (flash {:errors errors :data form}))})
        (>! pub-chan {:msg-type :valid-form :form valid-form}))
      (recur))))

(let [valid-form-chan (chan)]
  (sub publication :valid-form valid-form-chan)
  (async/thread
    (loop []
      (let [{:keys [form]} (<!! valid-form-chan)
            existing-username (users/find-user-by-username db-spec form)]
        (>!! pub-chan
             {:msg-type :existing-username :existing-username existing-username})
        (recur)))))

(let [valid-form-chan (chan)]
  (sub publication :valid-form valid-form-chan)
  (async/thread
    (loop []
      (let [{:keys [form]} (<!! valid-form-chan)
            existing-email (users/find-user-by-email db-spec form)]
        (>!! pub-chan {:msg-type :existing-email :existing-email existing-email})
        (recur)))))

(let [existing-username-chan (chan)
      existing-email-chan (chan)]
  (sub publication :existing-username existing-username-chan)
  (sub publication :existing-email existing-email-chan)
  (go-loop []
    (let [{:keys [existing-username]} (<! existing-username-chan)
          {:keys [existing-email]} (<! existing-email-chan)
          user-existed (boolean (or existing-username existing-email))]
      (>! pub-chan {:msg-type :whether-user-existed :user-existed user-existed})
      (recur))))

(let [whether-user-existed-chan (chan)
      valid-form-chan (chan)]
  (sub publication :whether-user-existed whether-user-existed-chan)
  (sub publication :valid-form valid-form-chan)
  (go-loop []
    (let [{:keys [user-existed]} (<! whether-user-existed-chan)
          {:keys [form]} (<! valid-form-chan)]
      (if user-existed
        (>! pub-chan {:msg-type :response
                      :response (-> (redirect "/daftar")
                                    (flash {:message user-existed-msg}))})
        (>! pub-chan {:msg-type :new-user :user form}))
      (recur))))

(let [new-user-chan (chan)]
  (sub publication :new-user new-user-chan)
  (async/thread
    (loop []
      (let [{:keys [password]} (:user (<!! new-user-chan))
            hashed-password (hashers/derive password)]
        (>!! pub-chan {:msg-type :hashed-password :password hashed-password})
        (recur)))))

(let [new-user-chan (chan)]
  (sub publication :new-user new-user-chan)
  (go-loop []
    (<! new-user-chan)
    (let [token (codecs/bytes->hex (nonce/random-bytes 16))]
      (>! pub-chan {:msg-type :email-verification-token :token token}))
    (recur)))

(let [hashed-password-chan (chan)
      email-verification-token-chan (chan)
      new-user-chan (chan)]
  (sub publication :hashed-password hashed-password-chan)
  (sub publication :email-verification-token email-verification-token-chan)
  (sub publication :new-user new-user-chan)
  (go-loop []
    (let [{:keys [user]} (<! new-user-chan)
          {:keys [password]} (<! hashed-password-chan)
          {:keys [token]} (<! email-verification-token-chan)
          prepared-user (assoc user :password password :token token)]
      (>! pub-chan {:msg-type :prepared-user :user prepared-user})
      (recur))))

(let [prepared-user-chan (chan)]
  (sub publication :prepared-user prepared-user-chan)
  (async/thread
    (loop []
      (let [{:keys [user]} (<!! prepared-user-chan)
            row-count (users/register-user db-spec user)]
        (if (zero? row-count)
          (>!! pub-chan {:msg-type :response
                         :response (-> (redirect "/daftar")
                                       (flash {:message failed-msg}))})
          (>!! pub-chan {:msg-type :persisted-user :user user}))
        (recur)))))

(let [persisted-user-chan (chan)]
  (sub publication :persisted-user persisted-user-chan)
  (async/thread
    (loop []
      (let [{:keys [user]} (<!! persisted-user-chan)]
        (mailer/send-email-verification user)
        (recur)))))

(let [persisted-user-chan (chan)]
  (sub publication :persisted-user persisted-user-chan)
  (go-loop []
    (<! persisted-user-chan)
    (>! pub-chan {:msg-type :response
                  :response (-> (redirect "/login")
                                (flash success-msg))})
    (recur)))

(defn register [{:keys [params]} respond _]
  (let [form (select-keys params [:username :email :password])]
    (go (let [[res _] (alts! [response-chan (timeout 10000)])]
          (respond (:response res))))
    (put! pub-chan {:msg-type :raw-input :form form})))


;; Email verification

(def no-token-msg "No token supplied.")

(def token-chan (chan))
(def v-response-chan (chan))

(async/thread
  (loop []
    (let [{:keys [token]} (<!! token-chan)
          row-count (users/verify-user db-spec token)]
      (if (zero? row-count)
        (>!! v-response-chan {:response (redirect "/login")})
        (>!! v-response-chan {:response "Verified"}))
      (recur))))

(defn verify [{:keys [params]} respond _]
  (let [token (:token params)]
    (if (nil? token)
      (respond (redirect "/login"))
      (do
        (put! token-chan {:msg-type :token :token token})
        (go (let [[res _] (alts! [v-response-chan (timeout 10000)])]
              (respond (:response res))))))))
