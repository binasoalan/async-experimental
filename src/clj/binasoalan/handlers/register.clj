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
                                                  alts! timeout put!
                                                  thread take!]]
            [ring.util.response :refer :all]))

(def msg {:user-existed "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain."
          :success "Anda sudah berjaya mendaftar. Sila check email untuk mengesahkan email anda."
          :failed "Pendaftaran gagal. Sila cuba semula."})


;; User registration

(defn- validate-form [[response-chan form]]
  (let [form-chan (chan)]
    (go
      (let [[errors valid-form] (v/validate-registration form)]
        (if errors
          (>! response-chan (-> (redirect "/daftar")
                                (flash {:errors errors :data form})))
          (>! form-chan form))))
    [response-chan form-chan]))

(defn- check-existing-user [[response-chan form-chan]]
  (let [user-chan (chan)]
    (go
      (let [form (<! form-chan)
            by-username (thread (users/find-user-by-username db-spec form))
            by-email (thread (users/find-user-by-email db-spec form))
            already-existed? (or (<! by-username) (<! by-email))]
        (if already-existed?
          (>! response-chan (-> (redirect "/daftar")
                                (flash {:message (:user-existed msg)})))
          (>! user-chan form))))
    [response-chan user-chan]))

(defn- persist-user [[response-chan user-chan]]
  (go
    (let [user (-> (<! user-chan)
                   (update :password hashers/derive)
                   (assoc :token (codecs/bytes->hex (nonce/random-bytes 16))))
          failed? (zero? (<! (thread (users/register-user db-spec user))))]
      (if failed?
        (>! response-chan (-> (redirect "/daftar")
                              (flash {:message (:failed msg)})))
        (do
          (future (mailer/send-email-verification user))
          (>! response-chan (-> (redirect "/login")
                                (flash {:message (:success msg)}))))))))

(defn register-handler [{:keys [params] :as req} respond _]
  (let [response-chan (chan)
        form (select-keys params [:username :email :password])]
    (go
      (->> [response-chan form]
           validate-form
           check-existing-user
           persist-user))
    (go (respond (<! response-chan)))))


;; Email verification

(defn- check-token [[response-chan token :as param]]
  (let [token-chan (chan)]
    (go
      (let [not-token? (->> (thread (users/find-token db-spec {:token token}))
                            <!
                            :token
                            nil?)]
        (if not-token?
          (>! response-chan (redirect "/login"))
          (>! token-chan token))))
    [response-chan token-chan]))

(defn- verify [[response-chan token-chan]]
  (go
    (let [token (<! token-chan)
          verified? (pos? (<! (thread
                                (users/verify-user db-spec {:token token}))))]
      (if verified?
        (>! response-chan (redirect "/verified"))
        (>! response-chan (redirect "/login"))))))

(defn verify-handler [{{token :token} :params :as req} respond _]
  (if (nil? token)
    (redirect "/login")
    (let [response-chan (chan)]
      (go
        (->> [response-chan token]
             check-token
             verify))
      (go (respond (<! response-chan))))))
