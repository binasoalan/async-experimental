(ns binasoalan.db
  (:require [binasoalan.config :as config]
            [buddy.core.codecs :as codecs]
            [buddy.core.crypto :as crypto]
            [environ.core :refer [env]]
            [hikari-cp.core :as hikari]))

(def username (config/decrypt (:database-username env)))
(def password (config/decrypt (:database-password env)))

(def datasource-options {:adapter "postgresql"
                         :database-name "binasoalan_dev"
                         :username username
                         :password password})

(def datasource (hikari/make-datasource datasource-options))

(def db-spec {:datasource datasource})
