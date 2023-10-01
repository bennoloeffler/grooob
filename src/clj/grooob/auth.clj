(ns grooob.auth
  (:require [grooob.db.core :as db]
            [buddy.hashers :as hashers]))


(defn create-user! [name email password]
  (if (db/find-user email)
    (throw (ex-info "User exists" {:email email :error :user-exists}))
    (db/create-user-without-pw-encrypt! name
                                        email
                                        (hashers/derive password {:alg :pbkdf2+sha256}))))

#_(defn create-user-google! [name email token]
    (if (db/find-user email)
      (throw (ex-info "User exists with password " {:email email :error :user-exists}))
      (db/create-user-without-pw-encrypt! name email (hashers/derive token))))

(comment
  (create-user! "Benno" "bel@belbel" "bel-pw"))

(defn authenticate-user [email password]
  (let [user   (db/find-user email)
        hashed (:user/password user)]
    (hashers/check password hashed)))


(comment
  (authenticate-user "bel@belbel" "bel-pw")
  (doall (db/find-user "bel@belbel")))


(comment
  (defn get-user [[key value]]
    (println key value))
  (get-user (vector {:k :v})))

