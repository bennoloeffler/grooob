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
    [playback.core] ; in order to be able to use #> #>> #><[] in the code
    [belib.core :as b]))



; #>      ; trace output
; #>>     ; trace output and input/bindings/steps (depending on the form)
; #>< _   ; reference currently selected portal data #><[]
; #>(defn ; makes functions replay with cached data on reload
; #>(defmethod,
; #>(>defn ;guardrails
(comment
  (require 'playback.preload) ; open the portal
  #>(defn make-something [a b]
      #>>(->> (range (* a b))
              (map inc)
              (map #(* 111 %))))
  (make-something 2 3)
  (println #><[])
  nil)



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

(defn rfa
  "refresh all"
  []
  (clojure.tools.namespace.repl/set-refresh-dirs
    "src/clj" "env/dev/clj")
  (refresh-all))

(defn t []
  (b/test-belib))

(comment
  (println (t))
  (rfa))

(comment
  ; https://stackoverflow.com/questions/15995350/display-loaded-dependencies-in-leiningen-repl
  (clojure.string/split (System/getProperty "java.class.path") #":"))


