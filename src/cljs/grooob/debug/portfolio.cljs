(ns grooob.debug.portfolio
  (:require
    [reagent.core :as r]
    ;[portfolio.reagent :refer-macros [defscene]]
    [portfolio.ui :as ui]))



(comment
  (p-start)
  (p-stop)
  (reset! ui-test true)
  (reset! ui-test false)
  :end)

(defn reload-home! []
  (-> js/window
      .-location
      .-href
      (set! "/#/")))

(comment
  (reload-home!))

(defonce ui-test (r/atom false))

(defn p-start []
  (reset! ui-test true))

(defn p-stop []
  (reset! ui-test false)
  (reload-home!))

(defn start []
  (ui/start! {:config {:css-paths ["/css/screen.css"]}}))
;:background/default-option-id :dark-mode}}))






