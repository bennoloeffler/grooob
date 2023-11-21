(ns grooob.comps.scenes
  (:require
    [portfolio.reagent :refer-macros [defscene]]
    [clojure.string :as str]
    [grooob.comps.ui]
    [belib.malli :as bm]
    [grooob.model.model-malli :as mm]
    [belib.date-time :as bd]
    [re-frame.core :as rf]
    [tick.core :as t]))



(defscene menu
          [grooob.comps.ui/overview-proj-details-menu])

