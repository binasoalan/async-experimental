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

(defn- check-existing-user [[_ form] out-ch]
  (->> [(async/thread (users/find-user-by-username db-spec form))
        (async/thread (users/find-user-by-email db-spec form))]
       (async/merge)
       (async/reduce #(boolean (or %1 %2)) false)
       (async/pipeline 1 out-ch (map #(vector % form)))))

(defn- generate-token []
  (codecs/bytes->hex (nonce/random-bytes 16)))

(defn- persist-user [[_ user] out-ch]
  (let [prepared-user (-> user
                          (update :password hashers/derive)
                          (assoc :token (generate-token)))]
    (->> (async/thread (users/register-user db-spec prepared-user))
         (async/pipeline 1 out-ch (map #(vector (zero? %) prepared-user))))))

(defn- split-if-error [ch]
  (async/split first ch))

(defn register-handler [{:keys [params] :as req} respond _]
  (let [input-ch (chan 1 (map v/validate-registration))
        [invalid-ch valid-ch] (split-if-error input-ch)

        availability-ch (chan)
        [not-available-ch available-ch] (split-if-error availability-ch)

        persisting-ch (chan)
        [failed-ch success-ch] (split-if-error persisting-ch)]

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

(defn- check-token [token out-ch]
  (->> (async/thread (users/find-token db-spec {:token token}))
       (async/reduce #(boolean (or %1 %2)) false)
       (async/pipeline 1 out-ch (map #(vector % token)))))

(defn- verify [[_ token] out-ch]
  (->> (async/thread (users/verify-user db-spec {:token token}))
       (async/pipeline 1 out-ch (map pos?))))

(defn verify-handler [{:keys [params] :as req} respond _]
  (if-let [token (:token params)]
    (let [input-ch (chan)

          validation-ch (chan)
          [valid-ch invalid-ch] (async/split first validation-ch)

          verification-ch (chan)
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
