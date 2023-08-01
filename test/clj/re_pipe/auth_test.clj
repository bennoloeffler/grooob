(ns re-pipe.auth-test
  (:require [clojure.test :refer :all]
            [re-pipe.db.core-test :refer :all]
            [re-pipe.db.core :as db]
            [re-pipe.auth :refer :all]))

;; see re-pipe.db.core-test
(use-fixtures :each conn-fixture)

(deftest create-user!-test
  (let [_ (create-user! "benno" "user@" "user-pw")
        u (db/find-user "user@")]
    (is (not= "user-pw" (:user/password u)))))


(require '[debux.core :refer :all])

(deftest authenticate-user-test
  (let [_ (create-user! "benno" "user@" "user-pw")
        u (db/find-user "user@")]
    (is (authenticate-user "user@" "user-pw"))))


