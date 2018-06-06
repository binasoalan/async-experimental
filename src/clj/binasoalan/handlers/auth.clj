(ns binasoalan.handlers.auth
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.db.users :as users]
            [binasoalan.utils.async :refer [fork fork-async]]
            [binasoalan.validation :as v]
            [binasoalan.views :as views]
            [buddy.hashers :as hashers]
            [clojure.core.async :as async :refer [chan go alt! put! close!]]
            [ring.util.http-response :refer :all]))


(defn- lookup-user
  "Lookup user from database and then compare the password. The result is a vector
  of which the first value is the error hashmap, and the second value is the
  user hashmap used previously to check. The result is pipelined to out-ch
  channel supplied in the second parameter."
  [[_ user] out-ch]
  (let [input-password (:password user)]
    (->> (users/find-user-by-username db-spec user)
         (merge {})
         (async/thread)
         (async/pipeline 1
                         out-ch
                         (comp
                          (map #(and (seq %)
                                     (hashers/check input-password (:password %))))
                          (map not)
                          (map #(vector % user)))))))

(defn- authenticate
  "Authenticate request by adding :identity key to session with username."
  [{:keys [params] :as request} _]
  (let [identity           (:username params)
        remember?          (:remember params)
        session            (:session request)
        updated-session    (assoc session :identity identity)
        authenticated-resp (-> (ok "Logged in")
                               (content-type "text/html")
                               (assoc :session updated-session))]
    (if remember?
      (assoc authenticated-resp :session-cookie-attrs {:max-age 31557600})
      authenticated-resp)))


(defn- invalid-response [request]
  (views/login request {:message "invalid input"}))

(defn- not-found-response [request]
  (views/login request {:message "wrong username/password"}))

(defn login-handler
  "Handler for user login."
  [{:keys [params] :as request} respond _]
  (let [input-ch                (chan)
        [invalid-ch valid-ch]   (fork (map v/validate-login) input-ch)
        [not-found-ch found-ch] (fork-async lookup-user valid-ch)
        auth-ch                 (->> [found-ch]
                                     (async/map (partial authenticate request)))]

    (put! input-ch params)

    (go
      (respond
       (alt!
         invalid-ch   ([]       (invalid-response request))
         not-found-ch ([]       (not-found-response request))
         auth-ch      ([result] result)))
      (close! input-ch))))


(defn logout-handler
  "Handler for user logout."
  [_ respond _]
  (respond (-> (found "/")
               (assoc :session {}))))
