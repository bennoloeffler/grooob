(ns grooob.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[grooob started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[grooob has shut down successfully]=-"))
   :middleware identity})
