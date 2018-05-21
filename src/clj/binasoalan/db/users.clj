(ns binasoalan.db.users
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "binasoalan/db/sql/users.sql")
