(ns binasoalan.handlers.register
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.mailer :as mailer]
            [binasoalan.utils :refer [flash split-if-error]]
            [binasoalan.validation :as v]
            [binasoalan.db.users :as users]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.core.async :as async :refer [go chan alt! put! close!]]
            [ring.util.response :refer :all]))

(def msg {:user-existed "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain."
          :success "Anda sudah berjaya mendaftar. Sila check email untuk mengesahkan email anda."
          :failed "Pendaftaran gagal. Sila cuba semula."})


;; User registration

(defn- check-existing-user
  "Check whether username or email already existed. The result is a vector where
  the first value is a boolean indicating username/email already existed, and
  the second value is the form previously used to check. The result is pipelined
  to out-ch channel supplied in second parameter."
  [[_ form] out-ch]
  (->> [(async/thread (users/find-user-by-username db-spec form))
        (async/thread (users/find-user-by-email db-spec form))]
       (async/merge)
       (async/reduce #(boolean (or %1 %2)) false)
       (async/pipeline 1 out-ch (map #(vector % form)))))

(defn- generate-token
  "Generate nonce as hexstring."
  []
  (codecs/bytes->hex (nonce/random-bytes 16)))

(defn- persist-user
  "Persist user into the database with hashed password and generated email
  verification token. The result is a vector where the first value is a boolean
  indicating whether database entries are added, and the second value is the
  user hashmap that was used to store in the database. The result is pipelined
  to out-ch channel supplied in second parameter."
  [[_ user] out-ch]
  (let [prepared-user (-> user
                          (update :password hashers/derive)
                          (assoc :token (generate-token)))]
    (->> (users/register-user db-spec prepared-user)
         (async/thread)
         (async/pipeline 1 out-ch (comp
                                   (map zero?)
                                   (map #(vector % prepared-user)))))))

(defn register-handler
  "Handler for user registration. The process goes through validation, checking
  for availability, and persisting data phases. When successful, email
  verification will be sent."
  [{:keys [params] :as req} respond _]
  (let [input-ch                        (chan 1 (map v/validate-registration))
        [invalid-ch valid-ch]           (split-if-error input-ch)
        availability-ch                 (chan)
        [not-available-ch available-ch] (split-if-error availability-ch)
        persisting-ch                   (chan)
        [failed-ch success-ch]          (split-if-error persisting-ch)]
    (->> valid-ch
         (async/pipeline-async 1 availability-ch check-existing-user))
    (->> available-ch
         (async/pipeline-async 1 persisting-ch persist-user))

    (put! input-ch params)

    (go
      (respond
       (alt!
         invalid-ch
         ([result] (-> (redirect "/daftar")
                       (flash {:errors (first result) :data (second result)})))

         not-available-ch
         ([]       (-> (redirect "/daftar")
                       (flash {:message (:user-existed msg)})))

         failed-ch
         ([]       (-> (redirect "/daftar")
                       (flash {:message (:failed msg)})))

         success-ch
         ([result] (do
                     (future (mailer/send-email-verification (second result)))
                     (-> (redirect "/login")
                         (flash {:message (:success msg)}))))))
      (close! input-ch))))


;; Email verification

(defn- validate-token [token]
  (if (seq token)
    [nil token]
    [{:error "invalid token"} token]))

(defn- lookup-token
  "Check if token actually exist in the database. The result is a vector where the
  first value is the error message, and the second value is the token previously
  used to check. The result is pipelined to out-ch channel supplied in second
  parameter."
  [token out-ch]
  (->> (users/find-token db-spec {:token token})
       (merge {})
       (async/thread)
       (async/pipeline 1 out-ch (map validate-token))))

(defn- verify
  "Verify user email based on token. The result is a vector where the first value
  is an indicator whether there is no problem with database update, and the
  second value is the token used previously to verify. The result is pipelined
  to out-ch channel supplied in second parameter."
  [[_ {:as token}] out-ch]
  (->> (users/verify-user db-spec token)
       (async/thread)
       (async/pipeline 1 out-ch (comp (map zero?) (map #(vector % token))))))

(defn verify-handler
  "Handler for email verification. The email verification token is validated
  before verification takes place."
  [{:keys [params] :as req} respond _]
  (if-let [token (:token params)]
    (let [input-ch               (chan)
          validation-ch          (chan)
          [invalid-ch valid-ch]  (split-if-error validation-ch)
          verification-ch        (chan)
          [failed-ch success-ch] (split-if-error verification-ch)]
      (->> input-ch
           (async/pipeline-async 1 validation-ch lookup-token))
      (->> valid-ch
           (async/pipeline-async 1 verification-ch verify))

      (put! input-ch token)

      (go
        (respond
         (alt!
           invalid-ch ([] (redirect "/login"))
           failed-ch  ([] (redirect "/login"))
           success-ch ([] (redirect "/verified"))))
        (close! input-ch)))
    (respond (redirect "/login"))))
