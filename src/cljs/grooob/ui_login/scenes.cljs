(ns grooob.ui-login.scenes
  (:require
    [portfolio.reagent :refer-macros [defscene]]
    [grooob.comps.ui]
    [grooob.ui-login.core]))





(defscene login
          [grooob.ui-login.core/login-form])

(defscene register
          [grooob.ui-login.core/register-form])

