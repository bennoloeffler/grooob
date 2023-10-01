(ns grooob.projects-overview.ui
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rd]
            [tick.core :as t]
            [java.time :refer [LocalDate]]
            [goog.events.KeyCodes :as keycodes]
            [goog.events :as events]
            [goog.object :as gobj]
            [goog.functions]
            [clojure.pprint :refer [pprint]]
            [belib.browser :as bb]
            [belib.spec :as bs]
            [grooob.grid-view.ui :as grid-view]
            [grooob.model.model-spec :as ms]
            [grooob.utils :as utils]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [grooob.projects-overview.events :as e]
            [grooob.re-comps.ui :as re-c])

  (:import goog.History
           [goog.events EventType KeyHandler]))


(defn projects-overview-form [component-id]
  (let [;cross   (rf/subscribe [:view/cross])
        ;model   (rf/subscribe [:model/model])
        _project (rf/subscribe [:model/model])
        ;id      (b/next-local-id)
        #_component-id #_(str "project-single-view-" id)]
    (rf/dispatch-sync [:grid-view/init component-id])
    (fn []
      [:<>
       [re-c/overview-proj-details-menu]
       #_[:div "project: " (str (last (get (vec (:projects @model)) (:project @cross))))]
       ;[:pre (with-out-str (pprint @model))]
       ;[:pre (with-out-str (pprint @project))]

       [grid-view/grid-form
        component-id
        _project
        (e/project-view-keydown-rules component-id _project)]])))

