(ns binasoalan.db.users
  (:require [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]))

(hugsql/def-db-fns "binasoalan/db/sql/users.sql")

(hugsql/def-db-fns "binasoalan/db/sql/email_verifications.sql")

(defn register-user
  "Register user by inserting to users and email_verifications tables.
  user must be a hashmap containing :username, :password, :email, and :token
  keys. Value for :password should be hashed. Value for :token should be a
  nonce. Returns number of rows affected."
  [db-spec user]
  (jdbc/with-db-transaction [tx db-spec]
    (+ (insert-user tx user)
       (insert-verification tx user))))

(defn verify-user
  "Verify user by setting verified to true. token must be a hashmap
  containing :token. Returns number of rows affected."
  [db-spec token]
  (jdbc/with-db-transaction [tx db-spec]
    (+ (verify-email tx token)
       (delete-verification tx token))))
