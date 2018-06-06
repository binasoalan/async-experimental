(ns binasoalan.handlers.auth
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.db.users :as users]
            [binasoalan.utils :refer [flash fork fork-async]]
            [binasoalan.validation :as v]
            [buddy.hashers :as hashers]
            [clojure.core.async :as async :refer [chan go alt! put! close!]]
            [ring.util.response :refer :all]))


(def invalid-response
  (-> (redirect "/login")
      (flash {:message "invalid input"})))

(def not-found-response
  (-> (redirect "/login")
      (flash {:message "wrong username/password"})))


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
  [{:keys [params] :as req} _]
  (let [identity           (:username params)
        remember?          (:remember params)
        session            (:session req)
        updated-session    (assoc session :identity identity)
        authenticated-resp (-> (response "Logged in")
                               (content-type "text/html")
                               (assoc :session updated-session))]
    (if remember?
      (assoc authenticated-resp :session-cookie-attrs {:max-age 31557600})
      authenticated-resp)))

(defn login-handler
  "Handler for user login."
  [{:keys [params] :as req} respond _]
  (let [input-ch                (chan)
        [invalid-ch valid-ch]   (fork (map v/validate-login) input-ch)
        [not-found-ch found-ch] (fork-async lookup-user valid-ch)
        auth-ch                 (async/map (partial authenticate req) [found-ch])]

    (put! input-ch params)

    (go
      (respond
       (alt!
         invalid-ch   ([]       invalid-response)
         not-found-ch ([]       not-found-response)
         auth-ch      ([result] result)))
      (close! input-ch))))


(defn logout-handler
  "Handler for user logout."
  [_ respond _]
  (respond (-> (redirect "/")
               (assoc :session {}))))
