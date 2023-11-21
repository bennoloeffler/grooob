(ns grooob.debug.portfolio
  (:require
    [reagent.core :as r]
    ;[portfolio.reagent :refer-macros [defscene]]
    [portfolio.ui :as ui]
    [grooob.debug.scenes]
    [grooob.comps.scenes]
    [grooob.comps.scenes-basic]
    #_[grooob.comps.scenes-entity]
    [grooob.ui-login.scenes]

    [grooob.debug.on-off-ui-tests :as on-off-ui-tests]))



(comment
  #_DUI
  (p-start)
  (p-stop)
  (reset! on-off-ui-tests/ui-test true)
  (reset! on-off-ui-tests/ui-test false)
  :end)

(defn reload-home! []
  (-> js/window
      .-location
      .-href
      (set! "/#/")))

(comment
  (reload-home!))


(defn p-start []
  (reset! on-off-ui-tests/ui-test true))

(defn p-stop []
  (reset! on-off-ui-tests/ui-test false)
  (reload-home!))

(defn start []
  (ui/start! {:config {:css-paths ["/css/screen.css"
                                   "/assets/font-awesome/css/all.css"]}}))
;:background/default-option-id :dark-mode}}))






