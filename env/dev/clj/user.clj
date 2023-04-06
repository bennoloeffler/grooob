(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
   [re-pipe.config :refer [env]]
   [clojure.pprint]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [mount.core :as mount]
   [clojure.tools.logging :as log]
   [re-pipe.core :refer [start-app]]
   [clojure.tools.namespace.repl :refer (refresh refresh-all clear)]
   [belib.core :as b]))


(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (log/debug "\n\nSTARTING grooob.com")
  (mount/start-without #'re-pipe.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'re-pipe.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/start))

(defn rf []
  (clojure.tools.namespace.repl/set-refresh-dirs
    "src/clj" "test/clj"
    "checkouts/belib/src" "checkouts/belib/test")
  (refresh-all))

(defn t []
  (b/test-belib))



