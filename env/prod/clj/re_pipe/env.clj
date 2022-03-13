(ns re-pipe.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[re-pipe started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[re-pipe has shut down successfully]=-"))
   :middleware identity})
