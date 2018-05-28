(ns binasoalan.migrations
  (:require [binasoalan.config :as config]
            [environ.core :refer [env]]
            [migratus.core :as migratus]))

(def database-name (:database-name env))
(def username (config/decrypt (:database-username env)))
(def password (config/decrypt (:database-password env)))

(def migratus-config {:store :database
                      :migration-dir "migrations"
                      :db {:dbtype "postgresql"
                           :dbname database-name
                           :user username
                           :password password}})

(defn -main
  "To be used for database migrations. Usage:

  lein run <task> [<id>|<migration-file-name>]

  task                - create, migrate, rollback, up, or down
  id                  - id for up/down task
  migration-file-name - file name for create task

  Refer migratus documentation for more details."
  [& args]
  (let [[task arg] args]
    (case task
      "create"   (migratus/create   migratus-config arg)
      "migrate"  (migratus/migrate  migratus-config)
      "rollback" (migratus/rollback migratus-config)
      "up"       (migratus/up       migratus-config arg)
      "down"     (migratus/down     migratus-config arg)
      (println
       "
Usage:

  lein run <task> [<id>|<migration-file-name>]

  task                - create, migrate, rollback, up, or down
  id                  - id for up/down task
  migration-file-name - file name for create task

Refer migratus documentation for more details.
"))))
