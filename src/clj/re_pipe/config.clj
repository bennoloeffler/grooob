(ns re-pipe.config
  (:require
    [clojure.string :as str]
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [mount.core :refer [args defstate] :as mount]))


(defn env-db-config
  "Constructs a datahike configuration map from heroku
  provided `DATABASE_URL` or returns nil if that env var is not present.
  ATTENTION: heroku delivers 'postgres' as dbtype.
  For jdbc, 'postgresql' is needed instead.
  So a perfect jdbc URL looks like:
  postgresql://user:pw@host:port/database.
  For usage with a localhost postgres installation (without pw), use:
  DATABASE_URL=postgres://benno:@localhost:5432/cream
  Details for heroku are there:
  https://devcenter.heroku.com/articles/connecting-to-relational-databases-on-heroku-with-java#using-the-jdbc_database_url"
  []
  (when-let [db-url (System/getenv "DATABASE_URL")]
    (let [uri (java.net.URI. db-url)
          [username password] (str/split (.getUserInfo uri) #":")]
      {:cfg-db {:store
                {:backend  :jdbc
                 :dbtype   "postgresql"
                 :host     (.getHost uri)
                 :user     username
                 :password password
                 :dbname   (str/join (drop 1 (.getPath uri)))
                 :port     (.getPort uri)}}})))

(defstate env
          :start
          (load-config
            :merge
            [(args)
             (or (env-db-config) {})
             (source/from-system-props)
             (source/from-env)]))

(comment
  (mount/start #'re-pipe.config/env))
