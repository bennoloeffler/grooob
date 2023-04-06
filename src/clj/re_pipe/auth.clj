(ns re-pipe.auth
  (:require [re-pipe.db.core :as db]
            [buddy.hashers :as hashers]))



(defn create-user! [name email password]
  (if (db/find-user-by-email email)
    (throw (ex-info "User exists" {:email email :error :user-exists}))
    (db/create-user! name email (hashers/derive password))))

#_(defn create-user-google! [name email token]
    (if (db/find-user-by-email email)
      (throw (ex-info "User exists with password " {:email email :error :user-exists}))
      (db/create-user! name email (hashers/derive token))))

(comment
  (create-user! "Benno" "bel@belbel" "bel-pw"))

(defn authenticate-user [email password]
  (let [user (vec (db/find-user-by-email email))
        hashed (last user)]
    (when (hashers/check password hashed)
      (take 3 user))))

(comment
  (authenticate-user "bel@belbel" "bel-pw")
  (seq (db/find-user-by-email "bel@belbel")))


(comment
  (defn get-user [[key value]]
    (println key value))
  (get-user (vector {:k :v})))

