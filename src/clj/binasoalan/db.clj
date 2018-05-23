(ns binasoalan.db
  (:require [hikari-cp.core :as hikari]))

(def datasource-options {:adapter "postgresql"
                         :database-name "binasoalan_dev"
                         :username "barebones"
                         :password "barebones"})

(def datasource (hikari/make-datasource datasource-options))

(def db-spec {:datasource datasource})
