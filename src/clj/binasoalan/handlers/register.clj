(ns binasoalan.handlers.register
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.mailer :as mailer]
            [binasoalan.utils :refer [flash common-interceptors]]
            [binasoalan.validation :as v]
            [binasoalan.views :as views]
            [binasoalan.db.users :as users]
            [buddy.hashers :as hashers]
            [buddy.core.codecs :as codecs]
            [buddy.core.nonce :as nonce]
            [clojure.core.async :as async :refer [go <!]]
            [ring.util.http-response :as resp]))

(def msg {:user-existed "Username/email sudah diambil. Sila daftar menggunakan username/email yang lain."
          :success "Anda sudah berjaya mendaftar. Sila check email untuk mengesahkan email anda."
          :failed "Pendaftaran gagal. Sila cuba semula."})


;; User registration


(defn- invalid-response
  [[errors data]]
  (-> (resp/found "/daftar")
      (flash {:errors errors :data data})))

(def not-available-response
  (-> (resp/found "/daftar")
      (flash {:message (:user-existed msg)})))

(def failed-response
  (-> (resp/found "/daftar")
      (flash {:message (:failed msg)})))

(def success-response
  (-> (resp/found "/login")
      (flash {:message (:success msg)})))


(defn- generate-token
  "Generate nonce as hexstring."
  []
  (codecs/bytes->hex (nonce/random-bytes 16)))


(def validate-input
  {:name ::validate-input
   :enter (fn [context]
            (let [[errors data :as validated] (-> context
                                                  :request
                                                  :form-params
                                                  v/validate-registration)]
              (if errors
                (assoc context :response (invalid-response validated))
                context)))})

(def check-existing-user
  {:name ::check-existing-user
   :enter (fn [context]
            (let [form           (get-in context [:request :form-params])
                  found-username (->> form
                                      (users/find-user-by-username db-spec)
                                      (async/thread))
                  found-email    (->> form
                                      (users/find-user-by-email db-spec)
                                      (async/thread))]
              (go
                (if (or (<! found-username)
                        (<! found-email))
                  (assoc context :response not-available-response)
                  context))))})

(def persist-user
  {:name ::persist-user
   :enter (fn [context]
            (let [user      (-> context
                                :request
                                :form-params
                                (update :password hashers/derive)
                                (assoc :token (generate-token)))
                  row-count (->> user
                                 (users/register-user db-spec)
                                 (async/thread))]
              (go
                (if-let [success? (pos? (<! row-count))]
                  (assoc context ::persisted-user user)
                  (assoc context :response failed-response)))))})

(def send-email-verification
  {:name ::send-email-verification
   :enter (fn [context]
            (let [user (::persisted-user context)]
              (future (mailer/send-email-verification user))
              (assoc context :response success-response)))})

(def register-interceptors (conj common-interceptors
                                 `validate-input
                                 `check-existing-user
                                 `persist-user
                                 `send-email-verification))


;; Email verification

(def validate-token
  {:name ::validate-token
   :enter (fn [context]
            (if-let [token (get-in context [:request :query-params :token])]
              context
              (assoc context :response (resp/found "/login"))))})

(def lookup-token
  {:name ::lookup-token
   :enter (fn [context]
            (let [token-exist? (->> context
                                    :request
                                    :query-params
                                    (users/find-token db-spec)
                                    (async/thread))]
              (go
                (if (<! token-exist?)
                  context
                  (assoc context :response (resp/found "/login"))))))})

(def verify
  {:name ::verify
   :enter (fn [context]
            (let [row-count (->> context
                                 :request
                                 :query-params
                                 (users/verify-user db-spec)
                                 (async/thread))]
              (go
                (if-let [success? (pos? (<! row-count))]
                  (assoc context :response (resp/found "/verified"))
                  (assoc context :response (resp/found "/login"))))))})

(def verify-email-interceptors (conj common-interceptors
                                     `validate-token
                                     `lookup-token
                                     `verify))


(def routes #{["/daftar" :get (conj common-interceptors `views/daftar)]
              ["/daftar" :post register-interceptors]
              ["/sahkan" :get verify-email-interceptors]})
