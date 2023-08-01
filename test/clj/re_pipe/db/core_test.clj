(ns re-pipe.db.core-test
  (:require [clojure.test :refer :all]
            [re-pipe.db.core :refer :all]
            [re-pipe.core]
            [re-pipe.handler]
            [mount.core :as mount]
            [datahike.api :as db]
            [hyperfiddle.rcf :refer [tests]])
  (:import [clojure.lang ExceptionInfo]
           [datahike.impl.entity Entity]))

(defn mount-start-test-db
  "mount test db"
  []
  (mount/start-with {#'re-pipe.db.core/conn     (connect-test-db)
                     #'re-pipe.core/http-server nil
                     #'re-pipe.core/repl-server nil
                     #'re-pipe.handler/init-app nil}))

(defn ss
  "use during developing tests to
  stop and start test conn and clear it that way."
  []
  (mount/stop)
  (mount-start-test-db))

(defn sstd
  "stop start and install test-data"
  []
  (ss)
  (install-test-data conn))

(defn conn-fixture
  "fixture for tests with a mocked in mem db and test data."
  [test]
  (mount-start-test-db)
  (install-test-data conn)
  (test)
  (mount/stop))

(use-fixtures :each conn-fixture)

(deftest install-schema-test
  (is (= (count (show-schema conn)) 13))
  (is (= (show-schema conn) [:model/data
                             :model/name
                             :user/email
                             :user/google-token
                             :user/models
                             :user/name
                             :user/password
                             :user/status
                             :user.status/active
                             :user.status/cancelled
                             :user.status/guest
                             :user.status/inactive
                             :user.status/pending]))
  (is (= (:user/password (find-user-by-email-raw "abc@example.com"))
         "secret")))


(deftest test-data-test
  (is (= (all-models-of-user "bel@bel.de")
         [{:db/id      17,
           :model/name "theModel",
           :model/data "dataDataData"}])))

(comment
  (test-data-test)
  (run-tests))

;;-----------------------------------------------------------
;; basic db api
;;-----------------------------------------------------------

(deftest find-entity-by-test
  (is (= (type (find-entity-by :user/email "bel@bel.de"))
         Entity))
  (is (number? (:db/id (find-entity-by :model/name "theModel")))))

;;-----------------------------------------------------------
;; user api
;;-----------------------------------------------------------

(deftest check-and-add-user-test
  (is (check-and-add-user "Benno"
                          :user.status/active
                          "bel2@bel.de"
                          "soSecret..."))

  ;; PASSWORT IS NOT HASHED, see re-pipe.auth
  (is (= (:user/password (find-entity-by :user/email "bel2@bel.de"))
         "soSecret..."))
  ;; try again...
  (is (thrown-with-msg? ExceptionInfo
                        #"bel2@bel.de.*already exists!"
                        (check-and-add-user
                          "NeuBennoNeu"
                          :user.status/pending
                          "bel2@bel.de"
                          "NotSoSecret..."))))


(deftest check-and-add-user-google!-test
  (is (check-and-add-user "Benno"
                          :user.status/active
                          "bel2@bel.de"
                          "soSecret..."))
  (is (check-and-add-user-google!
        :user.status/active
        "bel3@bel.de"
        {:some-token "xyz"}))
  ; TODO should the tokens be hashed or not? What for?
  (is (thrown-with-msg? ExceptionInfo
                        #"bel2.*password"
                        (check-and-add-user-google!
                          :user.status/active
                          "bel2@bel.de"
                          {:some-token "xyz"}))))

(deftest find-user-test
  (let [e        (find-user "bel@bel.de")
        e-as-map (entity-as-map e)
        test-map {:user/email    "bel@bel.de"
                  :user/name     "BEL"
                  :user/password "very-secret"
                  :user/status   (db/entity @conn 11)
                  :user/models   #{(db/entity @conn 17)}}]
    (is (= test-map e-as-map)))

  (is (not (find-user "NO-bel@bel.de"))))

(deftest del-user-test
  (is (del-user "bel@bel.de"))
  (is (not (find-user "bel@bel.de")))
  (is (thrown-with-msg? ExceptionInfo
                        #"bel@bel.de.*does not exist"
                        (del-user "bel@bel.de"))))

(deftest show-users-test
  (is (= 3 (count (show-users)))))

(deftest update-user-test
  (is (not (find-user "add-bel@bel.de")))
  (is (update-user {:user/name   "BEL"
                    :user/status :user.status/pending
                    :user/email  "add-bel@bel.de"}))
  (is (find-user "add-bel@bel.de"))
  (is (update-user {:user/name   "newBEL"
                    :user/status :user.status/active
                    :user/email  "add-bel@bel.de"}))
  (let [e (find-user "add-bel@bel.de")]
    (is (= "newBEL" (:user/name e)))
    (is (= :user.status/active (:db/ident (:user/status e)))))) ; its the ref, not the value

;;-----------------------------------------------------------
;; model api
;;-----------------------------------------------------------

(deftest all-models-test
  (is (= "theModel" (:model/name (first (all-models))))))

(deftest all-models-of-user-test
  (is (= "dataDataData"
         (-> (all-models-of-user "bel@bel.de")
             first
             :model/data))))

(deftest all-users-with-models-test
  (let [data (all-users-with-models)
        _    (println data)]

    (is (= 3 (count data)))
    (is (= "theModel"
           (-> data
               first
               :user/models
               first
               :model/name)))))

(deftest get-model-id-test
  (let [id (get-model-id "bel@bel.de" "theModel")]
    (is (= 17 id))))

(deftest add-model-test
  (add-model "bel@bel.de" "anotherModel" "modelData")
  (is (= 2
         (-> (all-models-of-user "bel@bel.de")
             count))))

(deftest remove-model-name-from-user-test
  (is (= 1
         (-> (all-models-of-user "bel@bel.de")
             count)))
  (add-model-test)
  (remove-model-name-from-user "bel@bel.de" "anotherModel")
  (is (= 1
         (-> (all-models-of-user "bel@bel.de")
             count)))
  (is (= "theModel"
         (:model/name (first (all-models-of-user "bel@bel.de"))))))


