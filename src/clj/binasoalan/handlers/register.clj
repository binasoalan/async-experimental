(ns binasoalan.handlers.register
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.mailer :as mailer]
            [binasoalan.utils :refer [flash]]
            [binasoalan.validation :as v]
            [binasoalan.db.users :as users]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.core.async :as async :refer [go chan alts! put! close!]]
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
    (->> (async/thread (users/register-user db-spec prepared-user))
         (async/pipeline 1 out-ch (map #(vector (zero? %) prepared-user))))))

(defn- split-if-error
  "Split the channel if the value from the channel contains error."
  [ch]
  (async/split first ch))

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

    (go
      (let [[val ch] (alts! [invalid-ch not-available-ch failed-ch success-ch])
            response (condp = ch
                       invalid-ch
                       (-> (redirect "/daftar")
                           (flash {:errors (first val) :data (second val)}))

                       not-available-ch
                       (-> (redirect "/daftar")
                           (flash {:message (:user-existed msg)}))

                       failed-ch
                       (-> (redirect "/daftar")
                           (flash {:message (:failed msg)}))

                       success-ch
                       (do
                         (future (mailer/send-email-verification (second val)))
                         (-> (redirect "/login")
                             (flash {:message (:success msg)}))))]
        (respond response)
        (close! input-ch)))

    (put! input-ch params)))


;; Email verification

(defn- check-token
  "Check if token actually exist in the database. The result is a vector where the
  first value is a boolean indicating whether the token indeed exists, and the
  second value is the token previously used to check. The result is pipelined to
  out-ch channel supplied in second parameter."
  [token out-ch]
  (->> (async/thread (users/find-token db-spec {:token token}))
       (async/reduce #(boolean (or %1 %2)) false)
       (async/pipeline 1 out-ch (map #(vector % token)))))

(defn- verify
  "Verify user email based on token. The result is an indicator whether database
  is updated. The result is pipelined to out-ch channel supplied in second
  parameter."
  [[_ token] out-ch]
  (->> (async/thread (users/verify-user db-spec {:token token}))
       (async/pipeline 1 out-ch (map pos?))))

(defn verify-handler
  "Handler for email verification. The email verification token is validated
  before verification takes place."
  [{:keys [params] :as req} respond _]
  (if-let [token (:token params)]
    (let [input-ch               (chan)
          validation-ch          (chan)
          [valid-ch invalid-ch]  (async/split first validation-ch)
          verification-ch        (chan)
          [success-ch failed-ch] (async/split identity verification-ch)]
      (->> input-ch
           (async/pipeline-async 1 validation-ch check-token))
      (->> valid-ch
           (async/pipeline-async 1 verification-ch verify))

      (go
        (let [[val ch] (alts! [invalid-ch failed-ch success-ch])
              response (condp = ch
                         invalid-ch (redirect "/login")
                         failed-ch (redirect "/login")
                         success-ch (redirect "/verified"))]
          (respond response)
          (close! input-ch)))

      (put! input-ch token))
    (respond (redirect "/login"))))
