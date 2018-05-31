(ns binasoalan.test-utils
  (:require [binasoalan.config :as config]
            [clojure.java.jdbc :as jdbc]
            [environ.core :refer [env]]))

(def path-driver "/home/burhanuddin/Downloads/geckodriver")

(defn route-to [route]
  (str "http://localhost:3000" route))


(def db-spec {:dbtype   "postgresql"
              :dbname   (:database-name env)
              :user     (config/decrypt (:database-username env))
              :password (config/decrypt (:database-password env))})

(defn- add-user []
  (jdbc/insert! db-spec
                :users
                {:username "hodor"
                 :email "burhanclj@gmail.com"
                 :verified true
                 ;; password = hodor123
                 :password "bcrypt+sha512$3b9c64c240dc7d65632b2ca91402878a$12$07494af8db79a581bdbb9ab8a528b670de7a2e1ecbd11acb"}))

(defn- remove-user []
  (jdbc/delete! db-spec
                :users
                ["username = ?" "hodor"]))

(defn with-user
  "Fixture for registered user."
  [f]
  (add-user)
  (f)
  (remove-user))

(defn- add-unverified-user []
  (jdbc/insert! db-spec
                :users
                {:username "hodorunverified"
                 :email "burhanunverified@gmail.com"
                 ;; password = hodor123
                 :password "bcrypt+sha512$3b9c64c240dc7d65632b2ca91402878a$12$07494af8db79a581bdbb9ab8a528b670de7a2e1ecbd11acb"}))

(defn- add-email-verification-token []
  (jdbc/insert! db-spec
                :email_verifications
                {:token "somerandomtoken"
                 :email "burhanunverified@gmail.com"}))

(defn- remove-unverified-user []
  (jdbc/delete! db-spec
                :users
                ["username = ?" "hodorunverified"]))

(defn with-unverified-user [f]
  (add-unverified-user)
  (add-email-verification-token)
  (f)
  (remove-unverified-user))
