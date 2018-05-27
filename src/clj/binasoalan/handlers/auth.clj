(ns binasoalan.handlers.auth
  (:require [binasoalan.db :refer [db-spec]]
            [binasoalan.db.users :as users]
            [binasoalan.utils :refer [flash]]
            [binasoalan.validation :as v]
            [buddy.hashers :as hashers]
            [clojure.core.async :as async :refer [chan go alts! put! close!]]
            [ring.util.response :refer :all]))


(defn- lookup-user
  "Lookup user from database and then compare the password. The result is a vector
  of which the first value is a boolean indicating whether they match, and the
  second value is the user hashmap used previously to check. The result is
  pipelined to out-ch channel supplied in the second parameter."
  [[_ user] out-ch]
  (let [password (:password user)]
    (->> (users/find-user-by-username db-spec user)
         (merge {})
         (async/thread)
         (async/pipeline 1
                         out-ch
                         (map #(if (seq %)
                                 [(hashers/check password (:password %)) user]
                                 [false user]))))))

(defn- authenticate
  "Authenticate request by adding :identity key to session with username."
  [req [_ user]]
  (let [remember?          (get-in req [:params :remember])
        session            (:session req)
        identity           (select-keys user [:username])
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
  (let [input-ch                (chan 1 (map v/validate-login))
        [invalid-ch valid-ch]   (async/split first input-ch)
        lookup-ch               (chan)
        [not-found-ch found-ch] (async/split (comp not first) lookup-ch)
        auth-ch                 (chan)]
    (->> valid-ch
         (async/pipeline-async 1 lookup-ch lookup-user))
    (->> found-ch
         (async/pipeline 1 auth-ch (map (partial authenticate req))))

    (go
      (let [[res ch] (alts! [invalid-ch not-found-ch auth-ch])
            response (condp = ch
                       invalid-ch
                       (-> (redirect "/login")
                           (flash {:message "invalid input"}))

                       not-found-ch
                       (-> (redirect "/login")
                           (flash {:message "wrong username/password"}))

                       auth-ch res)]
        (respond response)
        (close! input-ch)))

    (put! input-ch params)))


(defn logout-handler
  "Handler for user logout."
  [_ respond _]
  (respond (-> (redirect "/")
               (assoc :session {}))))
