(ns grooob.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [grooob.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "-=[grooob started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "-=[grooob has shut down successfully]=-"))
   :middleware wrap-dev})
