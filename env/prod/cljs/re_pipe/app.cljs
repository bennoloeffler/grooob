(ns re-pipe.app
  (:require [re-pipe.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
