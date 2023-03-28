(ns re-pipe.db.core
  (:require
    [datahike.api :as d]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [datahike-jdbc.core]
    ;[datomic.api :as d]
    ;[io.rkn.conformity :as c]
    [mount.core :refer [defstate] :as mount]
    [re-pipe.config :refer [env]]
    [clojure.tools.logging :as log]
    [hyperfiddle.rcf :refer [tests]])
  (:import [java.io PushbackReader]
           [java.util UUID]))


(hyperfiddle.rcf/enable! true)

(defn read-resource
  "Reads and returns data from a resource containing edn text. An
  optional argument allows specifying opts for clojure.edn/read"
  ([resource-name]
   (read-resource {:readers *data-readers*} resource-name))
  ([opts resource-name]
   (->> (io/resource resource-name)
        (io/reader)
        (PushbackReader.)
        (clojure.edn/read opts))))

(tests
  (def res (read-resource "migrations/schema.edn"))
  true := (some? res))

(defn apply-tx-from-file
  "Just applies the data in the file in one transaction."
  [conn file]
  (let [norms-map (read-resource file)]
    ;(println (str "transacting from file: " file))
    (pprint/pprint norms-map)
    (d/transact conn norms-map)
    #_(c/ensure-conforms conn norms-map (keys norms-map))))

(defn delete-db
  "Deletes the complete database including schema.
  Use :cfg-db from component env as config."
  []
  (-> env :cfg-db d/delete-database))

(comment
  (delete-db))

(defn connect-db
  "If there is a database, connect to it.
  If there is none, create it and connect to it.
  Use :cfg-db from component env as config."
  []
  (let [cfg (env :cfg-db)]
    (println "cfg: " cfg)
    (when-not (d/database-exists? cfg)
      (log/warn "going to create a new database: ")
      (log/info cfg)
      (d/create-database cfg))
    (d/connect cfg)))

(comment
  (with-redefs [env {:cfg-db {:name "bels-db"
                              :store {:backend :file :path "/tmp/example"}
                              :schema-flexibility :read}}]
    (connect-db)
    (delete-db)))

#_(defn show-schema
    "Show currently installed schema"
    [conn]
    (let [system-ns #{"db" "db.type" "db.install" "db.part"
                      "db.lang" "fressian" "db.unique" "db.excise"
                      "db.cardinality" "db.fn" "db.sys" "db.bootstrap"
                      "db.alter"}]
      (d/q '[:find ?ident
             :in $ ?system-ns
             :where
             [?e :db/ident ?ident]
             [(namespace ?ident) ?ns]]
           ;;[(not (contains? ?system-ns)) ?ns]]
           (d/db conn) system-ns)))


(defn show-schema
  "Show currently installed schema (all :db/ident's)."
  [conn]
  (-> (d/q '[:find ?ident
             :where
             [?e :db/ident ?ident]]
           (d/db conn))
      sort
      flatten))


(defn install-test-data
  [conn]
  (apply-tx-from-file conn "migrations/test-data.edn"))


(defn install-schema [conn]
  (apply-tx-from-file conn "migrations/schema.edn"))


(defn connect-and-schema-db
  "Create db, if not available.
  Connect to db.
  Install initial schema, if not already installed.
  Schema updates won't work that way.
  They need to be done manually."
  []
  (let [conn (connect-db)]
    (when (nil? (seq (show-schema conn)))
      (log/warn "Found no schema. Going to install initial schema...")
      (install-schema conn)
      (log/info (show-schema conn)))
    conn))

(defstate conn
          :start (connect-and-schema-db)
          :stop (-> conn d/release))

(comment
  (delete-db)
  (connect-db)
  (install-schema conn)
  (show-schema conn))

(defn start-conn []
  (mount/start #'re-pipe.config/env #'re-pipe.db.core/conn))

(defn stop-conn []
  (mount/stop #'re-pipe.config/env #'re-pipe.db.core/conn))

(comment
  (start-conn)
  (stop-conn)
  (install-test-data conn)
  (show-schema conn))

(defn show-users []
  (d/q '[:find [(pull ?e [:user/email :user/name :user/password {:user/status [:db/ident]}]) ...]
         :where
         [?e :user/email]]
       (d/db conn)))

(comment
  (show-users))

(defn add-user
  [conn {:keys [name status email]}]
  (d/transact conn [{:user/name   name
                     :user/status status
                     :user/email  email}]))


(comment
  (add-user conn
            {
             :name   "Bronco"
             :status :user.status/pending
             :email  "none"}))

(defn find-one-by
  "Given db value and an (attr/val), return the user as EntityMap (datomic.query.EntityMap)
   If there is no result, return nil.

   e.g.
    (d/touch (find-one-by (d/db conn) :user/email \"user@example.com\"))
    => show all fields
    (:user/first-name (find-one-by (d/db conn) :user/email \"user@example.com\"))
    => show first-name field"
  [db attr val]
  (d/entity db
            ;;find Specifications using ':find ?a .' will return single scalar
            (d/q '[:find ?e .
                   :in $ ?attr ?val
                   :where [?e ?attr ?val]]
                 db attr val)))

(comment
  (seq (find-one-by @conn :user/email "efg@example.com")))

(defn check-and-add-user
  "Adds new user to a database"
  [conn name status email password]
  (if-not (find-one-by (d/db conn) :user/email email)
    (d/transact conn [{
                       :user/name     name
                       :user/password password
                       :user/status   status
                       :user/email    email}])
    (throw (ex-info (str email " : this :user/email already exists!")
                    {:error "email exists" :email email}))))

(comment
  (check-and-add-user conn "Bruno Banani" :user.status/pending "bb@heaven.com" "pw"))

(defn find-user [db email]
  "Find user by email"
  (doall (find-one-by db :user/email email)))

(defn del-user [conn email]
  (if-let [db-id (:db/id (find-one-by (d/db conn) :user/email email))]
    (d/transact conn [[:db/retractEntity db-id]])
    (throw (ex-info (str email " : this :user/email does not exists!")
                    {:error "email does not exist" :email email}))))


(defn change-user
  "if not yet available: create.
  if email exists: overwrite the other attribs.
  IDENTICAL to implementation of add-user :-)"
  [conn email name status]
  (d/transact conn [{
                     :user/name   name
                     :user/status status
                     :user/email  email}]))

(comment
  (add-user conn
            {
             :name   "Bronco"
             :status :user.status/pending
             :email  "none"})
  (change-user conn "none"  "Bronco Bruno Berserker" :user.status/active)

  (if-let [db-id (:db/id (find-one-by (d/db conn) :user/email "none"))]
    (d/transact conn [:db/retractEntity db-id])
    (str "not deleted! found no user with email: " "none"))

  (del-user conn "none")
  (def id (:db/id (find-one-by @conn :user/email "none")))
  (d/transact conn {:tx-data [[:db/retractEntity id]]}))


(defn find-user-2 [conn email]
  "Find user by email"
  (when-let [id (find-one-by @conn :user/email email)]
    (println "id" id)
    (let [raw (d/pull @conn ; conn
                      '[:user/email :user/name {:user/status [:db/ident]} :user/password] ; pull string
                      (:db/id id))
          _ (println raw)
          {email :user/email name :user/name  status :user/status password :user/password} raw]
      [email name (:db/ident status) password])))




(comment
  (start-conn)
  (stop-conn)
  (apply-tx-from-file conn "migrations/schema.edn")
  (apply-tx-from-file conn "migrations/test-data.edn")
  (add-user conn {:email    "test@user"
                  :password "somepass"})
  (find-user @conn "bb@heaven.com")
  (d/pull @conn '[:user/email {:user/status [:db/ident]}] (:db/id (find-one-by @conn :user/email "efg@example.com")))
  (find-user-2 conn "bb@heaven.com")

  (def result #:user{:email "bb@heaven.com", :name "Bruno Banani", :status #:db{:ident :user.status/pending}})
  (let [{email :user/email {s :db/ident} :user/status} result]
    (println email (-> s name)))
  (name :user/status)
  (show-users))


(comment
  (show-schema conn))

(comment
  (with-redefs [env {:cfg-db {:name "bels-db"
                              :store {:backend :file :path "/tmp/example"}
                              :schema-flexibility :read}}
                conn (connect-and-schema-db)]
    (install-test-data conn)
    (count (show-schema conn)) := 9
    (delete-db)))


(defn populate-test-db []

  (install-test-data conn)
  (show-users))


#_(defn test-cycle []
    (let [started (= clojure.lang.Atom (type conn))
          _       (when started (mount/stop))
          _       (start-conn)
          _       (delete-db)
          _       (mount/stop)
          _       (start-conn)
          _       (install-test-schema conn)
          s       (show-schema conn)
          _       (populate-test-db)
          r       (find-user @conn "abc")
          _       (mount/stop)]
      [r s]))


;
;   user management
;

; find a user by email
(defn find-user-by-email [email]
  (find-user-2 conn email))

(comment
  (find-user (d/db conn) "bel@bennobel.de"))

; create a user
(defn create-user! [name email password]
  (check-and-add-user conn

                      name
                      :user.status/pending
                      email
                      password))

(comment
  (create-user! "Benno" "bel" "pw"))

; login user

