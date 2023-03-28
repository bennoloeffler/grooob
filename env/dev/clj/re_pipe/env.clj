(ns re-pipe.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [re-pipe.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "-=[re-pipe started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "-=[re-pipe has shut down successfully]=-"))
   :middleware wrap-dev})
