(ns re-pipe.db.core
  (:require
    [puget.printer :refer [cprint]]
    [clojure.string :as str]
    [tick.core :as t]
    [belib.core :as bc]
    [datahike.api :as d]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [datahike-jdbc.core]
    ;[datomic.api :as d]
    ;[io.rkn.conformity :as c]
    [mount.core :refer [defstate only swap-states] :as mount]
    [re-pipe.config :refer [env]]
    [clojure.tools.logging :as log]
    [hyperfiddle.rcf :refer [tests]]
    [belib.core :as b]
    [buddy.hashers :as hashers]
    [time-literals.read-write]
    [playback.core])
  (:import [java.io PushbackReader]
           [java.util UUID]))

; #>      ; trace output
; #>>     ; trace output and input/bindings/steps (depending on the form)
; #>< _   ; reference currently selected portal data #><[]
; #>(defn ; makes functions replay with cached data on reload
; #>(defmethod,
; #>(>defn ;guardrails
(comment
  (require 'playback.preload)
  #>>(defn make-something [a b]
       #>>(->> (range (* a b))
               (map inc)
               (map #(* 11 %))
               (str/join)))
  (make-something 2 3)
  (println #><[])
  nil)

(time-literals.read-write/print-time-literals-clj!)

(hyperfiddle.rcf/enable! false)

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
  (let [data (read-resource file)]
    ;(println (str "transacting from file: " file))
    ;(pprint/pprint data)
    (d/transact conn data)))


(defn delete-db
  "Deletes the complete database including schema.
  Use :cfg-db from component env as config."
  []
  (-> env :cfg-db d/delete-database))

(comment
  (delete-db))

(defn install-schema [conn]
  (apply-tx-from-file conn "migrations/schema.edn"))


(def test-cfg
  "config for test db in memory"
  {:store              {:backend :mem
                        :id      "bels-db"}
   :schema-flexibility :read})


(defn delete-test-db
  "Deletes the test db."
  []
  (d/delete-database test-cfg))

(defn connect-test-db
  "starts empty test db."
  []
  (when (d/database-exists? test-cfg)
    (delete-test-db))
  (d/create-database test-cfg)
  (let [conn (d/connect test-cfg)]
    (install-schema conn)
    conn))

(comment
  (def c (connect-test-db))
  (delete-test-db))


(defn connect-db
  "If there is a database, connect to it.
  If there is none, create it and connect to it.
  Use :cfg-db from component env as config."
  []
  (let [cfg (env :cfg-db)]
    ;(println "cfg: " cfg)
    (when-not (d/database-exists? cfg)
      (log/warn "going to create a new database: ")
      (log/info cfg)
      (d/create-database cfg))
    (d/connect cfg)))


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




(comment
  (with-redefs [env {:cfg-db {:name               "bels-db"
                              :store              {:backend :file :path "/tmp/example"}
                              :schema-flexibility :read}}]))




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

#_(defn start-conn []
    (mount/start #'re-pipe.config/env #'re-pipe.db.core/conn))

#_(defn stop-conn []
    (mount/stop #'re-pipe.config/env #'re-pipe.db.core/conn))

(defn conn-running? []
  ((mount/running-states) (str #'conn)))

(comment
  ;(start-conn)
  ;(stop-conn)
  (connect-db)
  (delete-db)
  (connect-db)
  (install-schema conn)
  (show-schema conn)
  (install-test-data conn)
  (show-schema conn)
  (conn-running?))


(defn show-users []
  (d/q '[:find [(pull ?e [:user/email :user/name :user/password {:user/status [:db/ident]} :user/models]) ...]
         :where
         [?e :user/email]]
       (d/db conn)))

(comment
  (show-users))

#_(defn add-user
    "Very simple add and update function used with
  simplified keys: :name :status :email.
  :email will be the unique key for update."
    [{:keys [name status email]}]
    (d/transact conn [{:user/name   name
                       :user/status status
                       :user/email  email}]))

#_(comment
    (add-user
      {:name   "Bronco"
       :status :user.status/pending
       :email  "none"}))

(defn find-entity-by
  "Given an attr/val, return Entity.
  If there is no result, return nil.
  e.g.
    (seq (find-entity-by @conn :user/email \"user@example.com\"))
    => show all fields
    (:user/first-name (find-entity-by [...]))
    => show first-name field"
  [attr val]
  (d/entity @conn
            ;;find Specifications using ':find ?a .' will return single scalar
            (d/q '[:find ?e .
                   :in $ ?attr ?val
                   :where [?e ?attr ?val]]
                 @conn attr val)))

(comment
  (:user/email (find-entity-by :user/email "loeffler@v-und-s.de" #_"efg@example.com")))

(defn check-and-add-user
  "Adds new user to a database"
  [name status email password]
  (if-not (find-entity-by :user/email email)
    (d/transact conn [{
                       :user/name     name
                       :user/password password
                       :user/status   status
                       :user/email    email}])
    (throw (ex-info (str email " : this :user/email already exists!")
                    {:error "email exists" :email email}))))

(comment
  ;; Generate hash from plain password
  (hashers/derive "secretpassword" {:alg :pbkdf2+sha256})
  ;; => "pbkdf2+sha256$4i9sd34m..."

  (hashers/check "secretpassword" "pbkdf2+sha256$a4d026ce5812b7744d7dcf90$100000$8ea198216c09b4c14fe69341e1a36c2aafbdd9159e61fc0282a1f0a02e26eabb"))
;; => {:valid true :update false})

(defn check-and-add-user-google!
  "Adds new user with google token
  if not already exists with local password."
  [status email tokens]
  ; token looks like:
  ;{:google {:expires #<org.joda.time.DateTime@3f642d03 2023-07-23T16:02:28.737Z>,
  ;          :extra-data {:scope "openid https://www.googleapis.com/auth/userinfo.email",
  ;                       :token_type "Bearer"},
  ;          :id-token "eyJhbGciOiJSUzI1NiIsImtpZCI6ImEzYmRiZmRlZGUzYmFiYjI2NTFhZmNhMjY3OGRkZThjMGIzNWRmNzYiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiI2MDczOTcyNjEwMTktNGZrYmMyNWNoZ25vaG5sZGRqanFsM2w0dmY3YnU1dmYuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI2MDczOTcyNjEwMTktNGZrYmMyNWNoZ25vaG5sZGRqanFsM2w0dmY3YnU1dmYuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDI0NTQ3MzIyMTc4OTgxNDQ0NjkiLCJlbWFpbCI6ImxvZWZmbGVyQHYtdW5kLXMuZGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXRfaGFzaCI6IlowWVE0dllkZDRwTWlhbmhXaGVaNkEiLCJpYXQiOjE2OTAxMjQ1NDksImV4cCI6MTY5MDEyODE0OX0.propiHJa6pWOywpWApjtUq4J1VcWQ_DMk3ZG6B29ZtZ6vOB3OxXKaND7gH471xdFcHRWiI40RvCWQiOajrJuyFHP0w0oJtkwSh51Q5UhkAospEto87K9R3XmZlFTUsp4-eYszH0McxEdJ1371n6VrFvM9mkTrL-JoT_i59l0Plc8iYi98RRGOL--bZhUmarhldT_Ss5daWPRmqp4PpiwJSXlUmtZJkCYIpqVntKzvR__krImvwD7Q_h2zhdJ6LHwu0iel5FwVq-qu0jj0M834b7sStSVNwZrJzIstJeil-IdcxGHxVSdcBrBjvIe4XNIsmT5JwCrmBaGPwSrj5Ea9A",
  ;          :token "ya29.a0AbVbY6PFWfePnQ7BtaHNCwG_ydfvilhp__H9pRKfP1wwJ230Em9apcuP9--0GZ-s-2Ix7Ylon83pLK5Q10H3Nyvs_OFW6a4qN-NYR2UyygLaGsKUnLl07eGwyjnhEuuuf6mnlC92XHBlN6zEkSzb0iCGV1KqaCgYKAUsSARMSFQFWKvPld40CbhjaYiLyAnNSf8AxEA0163"]
  ; save and read with: (pr-str x) and (read-string (pr-str x))
  ; :oauth2/access-tokens {:google...
  (let [user     (find-entity-by :user/email email)
        password (:user/password user)
        tokens   (if (-> tokens :google :expires)
                   ; the :expires date is a org.joda.time.DateTime
                   ; change it to tick, so it can be written and read again.
                   (update-in tokens [:google :expires] #(t/offset-date-time (str %)))
                   ; if there is no :expires token (eg in tests):
                   ; ignore and just return unchanged
                   tokens)]
    (if password
      (throw (ex-info (str email ", :user/email already exists with password!")
                      {:error "email exists with password" :email email}))
      (do
        (when user
          (log/info "user" email "exists already with google-token... updating"))
        (d/transact conn [{:user/google-token (pr-str tokens) #_(hashers/derive (pr-str tokens) {:alg :pbkdf2+sha256})
                           :user/status       status
                           :user/email        email}])))))

(comment
  (t/offset-date-time (str (org.joda.time.DateTime.)))
  (read-string (pr-str {:token "something"}))
  (check-and-add-user "Bruno Banani" :user.status/pending "bb@heaven.com" "pw"))

(defn find-user
  "Find user by email.
  Returns a Entity, that behaves like a map.
  When printed, only shows id.
  Use doall or seq or something to show everything."
  [email]
  (find-entity-by :user/email email))

(defn del-user [email]
  (if-let [db-id (:db/id (find-entity-by :user/email email))]
    (d/transact conn [[:db/retractEntity db-id]])
    (throw (ex-info (str email " : this :user/email does not exists!")
                    {:error "email does not exist" :email email}))))


(defn update-user
  "If :user/email not yet available: create.
  If :user/email exists: overwrite the other attribs."
  [tx-data]
  (assert (:user/email tx-data))
  (d/transact conn [tx-data]))

(comment
  #_(add-user
      {:name   "Bronco"
       :status :user.status/pending
       :email  "none"})
  (update-user {:user/name   "Bronco Bruno Berserker"
                :user/status :user.status/active
                :user/email  "none"})

  (if-let [db-id (:db/id (find-entity-by :user/email "none"))]
    (d/transact conn [:db/retractEntity db-id])
    (str "not deleted! found no user with email: " "none"))

  (del-user "none")
  (def id (:db/id (find-entity-by :user/email "none")))
  (d/transact conn {:tx-data [[:db/retractEntity id]]}))


#_(defn find-user-2 [email]
    "Find user by email"
    (when-let [id (find-entity-by :user/email email)]
      ;(println "id" id)
      (let [raw (d/pull @conn
                        '[:user/email :user/name {:user/status [:db/ident]} :user/password] ; pull string
                        (:db/id id))
            ;_ (println raw)
            {email :user/email name :user/name status :user/status password :user/password} raw]
        [email name (:db/ident status) password])))

(defn find-user-by-email-raw [email]
  "Find user by email. Returns result of pull"
  (when-let [id (find-entity-by :user/email email)]
    ;(println "id" id)
    (let [raw (d/pull @conn
                      '[:user/email :user/name {:user/status [:db/ident]} :user/password :user/google-token] ; pull string
                      (:db/id id))]
      ;_ (println raw)
      ;{email :user/email name :user/name  status :user/status password :user/password} raw]
      raw
      #_[email name (:db/ident status) password])))



(comment
  (start-conn)
  (stop-conn)
  (apply-tx-from-file conn "migrations/schema.edn")
  (apply-tx-from-file conn "migrations/test-data.edn")
  (update-user {:email    "test@user"
                :password "somepass"})
  (seq (find-user "bb@heaven.com"))
  (d/pull @conn '[:user/email {:user/status [:db/ident]}] (:db/id (find-entity-by :user/email "bel@belbel")))
  #_(find-user-2 "bb@heaven.com")

  (def result #:user{:email "bb@heaven.com", :name "Bruno Banani", :status #:db{:ident :user.status/pending}})
  (let [{email :user/email {s :db/ident} :user/status} result]
    (println email (-> s name)))
  (name :user/status)
  (show-users))


(comment
  (show-schema conn))

#_(comment
    (with-redefs [env  {:cfg-db {:name               "bels-db"
                                 :store              {:backend :file :path "/tmp/example"}
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
#_(defn find-user [email]
    (find-user email))

#_(comment
    (find-user "bel@bennobel.de"))

; create a user
(defn create-user-without-pw-encrypt! [name email password]
  (check-and-add-user name
                      :user.status/pending
                      email
                      password))

(defn check-user-google
  "is there a user with google-token"
  [email]
  (let [user   (find-user-by-email-raw email)
        ;_      (println "the tokens from db:")
        ;_      (cprint (:user/google-token user))
        tokens (read-string (:user/google-token user))]
    (and tokens user)))

(defn mount-start-test-db-manual []
  (-> (only #{#'conn})
      (swap-states {#'conn {:start #(connect-test-db)
                            :stop  #(delete-test-db)}})

      mount/start))


(comment
  (mount-start-test-db-manual)
  (install-test-data conn)
  (mount/stop)
  conn
  (start-conn)
  (stop-conn)

  (delete-db)
  (connect-db)
  (install-schema conn)
  (show-schema conn)

  (create-user-without-pw-encrypt! "Benno" "bel2" "pw")
  (check-and-add-user-google! "Benno" "bel3" "token")
  (type (find-user-by-email-raw "bel@bel.de"))
  (type {})
  (del-user "loeffler@v-und-s.de")
  (find-user-by-email-raw "bel3")
  #>(find-user "bel3")
  (:db/ident (d/q @conn (:db/id #><[]))))


;;----------------------------------------------------------------------------
;;  model API
;;----------------------------------------------------------------------------

(defn all-models
  "Returns all models with name and data
  as string (extract to data with read-string)."
  []
  (d/q '[:find [(pull ?e [:db/id :model/name :model/data]) ...]
         :where
         [?e :model/name]]
       (d/db conn)))


(defn- all-models-of-user-intern
  "Returns all models of user with email.
  Model contains id, name and data as string
  (extract to data with read-string)."
  [email]
  (d/q '[:find [(pull ?e [:db/id
                          :user/name
                          :user/email
                          {:user/models [:db/id
                                         :model/name
                                         :model/data]}]) ...] ; ... to be used from all-users-with-models
         :in $ ?email
         :where
         [?e :user/email ?email]]
       (d/db conn) email))

(defn all-models-of-user [email]
  (:user/models (first (all-models-of-user-intern email))))

(comment
  (all-models-of-user nil))

(defn all-users-with-models
  "Return all users with their attached models."
  []
  (all-models-of-user-intern nil))

(defn get-model-id
  "return :db/id of a model with model-name of user with email.
  The combination of email and model-name may be interpreted as unique-id."
  [email model-name]
  (d/q '[:find ?e .
         :in $ ?email ?model-name
         :where
         [?e :model/name ?model-name]
         [?u :user/email ?email]
         [?u :user/models ?e]]
       (d/db conn) email model-name))

(defn add-model
  "used as
  1 create model and user
  2 create model and add to an existing user
  3 update model.
  If email and model-name exist already, just update the data.
  The combination of email and model-name may be interpreted as unique-id."
  [email model-name model-data]
  (let [model-id (get-model-id email model-name)]
    (println model-id)
    (if model-id
      ;; ONLY update model, because there is already a ref from user
      (d/transact conn [{:db/id      model-id
                         :model/data model-data}])
      ;; create model and add ref from user, because there was not yet a ref from the user to the model-name
      (d/transact conn [{:db/id      -99 ; tmpids
                         :model/name model-name
                         :model/data model-data}
                        {:user/models -99
                         :user/email  email}]))))

(defn remove-model-id-from-user
  ":user/models can hold many models.
  If model-id exists as reference, retract it."
  [email model-id]
  (assert (some? email))
  (assert (and (some? model-id) (number? model-id) (pos? model-id)))
  (d/transact conn [[:db/retract [:user/email email] :user/models model-id]]))


(comment
  (d/transact conn [{:db/id      99
                     :model/name "ModelName-90"
                     :model/data "modelData-90"}
                    {:user/email  "loeffler@v-und-s.de"
                     :user/models 99}])
  (get-model-id "loeffler@v-und-s.de" "ModelName-90")
  (all-models-of-user "loeffler@v-und-s.de")
  (remove-model-id-from-user "loeffler@v-und-s.de" 99))


(defn remove-model-name-from-user
  "if there is a model with model-name referenced from user with email, remove it.
  The model entity is not retracted.
  Only the ref from user to model is retracted.
  If there is no model-name or no user email: do nothing."
  [email model-name]
  (let [model-id (get-model-id email model-name)]
    (when model-id (remove-model-id-from-user email model-id))))


(comment
  (all-users-with-models)
  (all-models)

  (remove-model-name-from-user "loeffler@v-und-s.de" "-ModelName-90")
  (remove-model-name-from-user "loeffler@v-und-s.de" "ModelName-90")

  (d/transact conn [{:db/id      -99
                     :model/name "ModelName-91"
                     :model/data "modelData-91"}
                    {:user/email  "loeffler@v-und-s.de"
                     :user/models -99}])


  (d/q '[:find [(pull ?e [:db/id :model/name :model/data]) ...]
         :where
         [?e :model/name]]
       (d/db conn))
  (d/q '[:find [(pull ?e [:db/id
                          :user/name
                          :user/email
                          {:user/models [:db/id
                                         :model/name
                                         :model/data]}]) ...]
         :where
         [?e :user/email]]
       (d/db conn))


  (add-model "loeffler@v-und-s.de" "a-new-Model33" "datadatadata")

  (all-models-of-user "tietz@v-und-s.de")
  (add-model "tietz@v-und-s.de" "anotherM2" "--datadatadata")
  (add-model "tietz@v-und-s.de" "anotherM3" "NEWnewDatadatadatadata")
  (get-model-id "tietz@v-und-s.de" "anotherM3")
  (remove-model-id-from-user "tietz@v-und-s.de" 113)

  (println (b/test-belib))
  (show-users)
  (create-user-without-pw-encrypt! "Benno" "loeffler@v-und-s.de" "pw")
  (find-user-by-email-raw "loeffler@v-und-s.de")
  (type (:date (read-string (pr-str {:date (t/date "2023-04-05")}))))

  (add-model "loeffler@v-und-s.de" "abc" (pr-str {:date (t/date "2023-04-05")}))
  (add-model "loeffler@v-und-s.de" "def" (pr-str {:other-date (t/date "2012-03-04")}))
  (find-user "loeffler@v-und-s.de")
  (del-user "loeffler@v-und-s.de"))

(comment
  (let [_ (mount/start-with {#'re-pipe.db.core/conn     (connect-test-db)
                             #'re-pipe.core/http-server nil
                             #'re-pipe.core/repl-server nil
                             #'re-pipe.handler/init-app nil})]
    #_(when (conn-running?) (stop-conn))
    #_(start-conn)
    (#_delete-db)
    #_(stop-conn)
    #_(start-conn)
    (assert (= (count (show-schema conn)) 13))
    (install-test-data conn)
    (assert (= (:user/password (find-user-by-email-raw "abc@example.com")) "secret"))
    (mount/stop))
  nil)

(defn entity-as-map [e]
  (reduce (fn [acc val] (into acc {val (val e)}))
          {}
          (keys e)))
