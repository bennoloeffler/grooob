(ns grooob.debug.scenes
  (:require
    [portfolio.reagent :refer-macros [defscene]]
    [grooob.re-comps.ui]
    [grooob.core]))


(defscene button
          [:button.button "Click me"])

(defscene menu
          [grooob.re-comps.ui/overview-proj-details-menu])

(defscene login
          [grooob.core/login-form])

