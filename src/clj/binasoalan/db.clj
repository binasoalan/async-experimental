(ns binasoalan.db
  (:require [binasoalan.config :as config]
            [environ.core :refer [env]]
            [hikari-cp.core :as hikari]))

(def database-name (:database-name env))
(def username (config/decrypt (:database-username env)))
(def password (config/decrypt (:database-password env)))

(def datasource-options {:adapter "postgresql"
                         :database-name database-name
                         :username username
                         :password password})

(def datasource (hikari/make-datasource datasource-options))

(def db-spec {:datasource datasource})
