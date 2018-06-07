(ns binasoalan.handlers.register
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.handlers.pages :as pages]
            [binasoalan.mailer :as mailer]
            [binasoalan.utils.async :refer [fork fork-async]]
            [binasoalan.validation :as v]
            [binasoalan.db.users :as users]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.core.async :as async :refer [go chan alt! put! close!]]
            [ring.util.http-response :refer :all]))

(def msg {:user-existed "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain."
          :success "Anda sudah berjaya mendaftar. Sila check email untuk mengesahkan email anda."
          :failed "Pendaftaran gagal. Sila cuba semula."})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; User registration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(def x-validate
  (map v/validate-registration))

(defn- invalid-response [request [errors data :as result]]
  (pages/daftar request {:errors errors :data data}))

(defn- not-available-response [request]
  (pages/daftar request {:message (:user-existed msg)}))

(defn- failed-response [request]
  (pages/daftar request {:message (:failed msg)}))

(defn- success-response [request]
  (pages/login request {:message (:success msg)}))

(defn register-handler
  "Handler for user registration. The process goes through validation, checking
  for availability, and persisting data phases. When successful, email
  verification will be sent."
  [{:keys [params] :as request} respond _]
  (let [input-ch                        (chan)
        [invalid-ch valid-ch]           (fork x-validate input-ch)
        [not-available-ch available-ch] (fork-async check-existing-user valid-ch)
        [failed-ch success-ch]          (fork-async persist-user available-ch)]

    (put! input-ch params)

    (go
      (respond
       (alt!
         invalid-ch       ([result] (invalid-response request result))
         not-available-ch ([]       (not-available-response request))
         failed-ch        ([]       (failed-response request))
         success-ch       ([[_ user]]
                           (future (mailer/send-email-verification user))
                           (success-response request))))
      (close! input-ch))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Email verification
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- lookup-token
  "Check if token actually exist in the database. The result is a vector where the
  first value is an indicator for error, and the second value is the token
  previously used to check. The result is pipelined to out-ch channel supplied
  in second parameter."
  [token out-ch]
  (->> (users/find-token db-spec {:token token})
       (merge {})
       (async/thread)
       (async/pipeline 1 out-ch (comp
                                 (map #(not (seq %)))
                                 (map #(vector % token))))))

(defn- verify
  "Verify user email based on token. The result is a vector where the first value
  is an indicator whether there is no problem with database update, and the
  second value is the token used previously to verify. The result is pipelined
  to out-ch channel supplied in second parameter."
  [[_ token] out-ch]
  (->> (users/verify-user db-spec {:token token})
       (async/thread)
       (async/pipeline 1 out-ch (comp
                                 (map zero?)
                                 (map #(vector % token))))))

(defn verify-handler
  "Handler for email verification. The email verification token is validated
  before verification takes place."
  [{:keys [params] :as request} respond _]
  (if-let [token (:token params)]
    (let [input-ch               (chan)
          [invalid-ch valid-ch]  (fork-async lookup-token input-ch)
          [failed-ch success-ch] (fork-async verify valid-ch)]

      (put! input-ch token)

      (go
        (respond
         (alt!
           invalid-ch ([] (see-other "/login"))
           failed-ch  ([] (see-other "/login"))
           success-ch ([] (found "/verified"))))
        (close! input-ch)))
    (respond (see-other "/login"))))
