(ns re-pipe.debug.scenes
  (:require
    [portfolio.reagent :refer-macros [defscene]]
    [re-pipe.re-comps.ui]
    [re-pipe.core]))


(defscene button
          [:button.button "Click me"])

(defscene menu
          [re-pipe.re-comps.ui/overview-proj-details-menu])

(defscene login
          [re-pipe.core/login-form])

