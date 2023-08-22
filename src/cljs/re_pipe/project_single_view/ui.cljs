(ns re-pipe.project-single-view.ui
  (:require
    [belib.core :as b]
    [re-frame.core :as rf]
    [re-pipe.re-comps.ui :as re-c]
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
    [re-pipe.grid-view.ui :as grid-view]
    [re-pipe.model-spec :as ms]
    [belib.cal-week-year :as bc]
    [ajax.core :as ajax]
    [goog.history.EventType :as HistoryEventType]
    [time-literals.read-write]
    [luminus-transit.time :as time]
    [cognitect.transit :as transit]
    [re-pressed.core :as rp]
    [re-pipe.utils :as utils]
    [re-pipe.project-single-view.events :as e]
    ;[re-pipe.project-single-view.core :as c]
    [cuerdas.core :as str])

  (:import goog.History
           [goog.events EventType KeyHandler]))


; TODO
; scrolling at component-id



#_(defn test-params [p]
    (let [x (str p "--" p)]
      (fn [] [:div (str "params: " x)])))

(defn project-single-view [component-id]
  (let [;; TODO this :projects-overview-form s path is HARDCODED!
        _project (rf/subscribe [:model/current-project "projects-overview-form"])]
    (rf/dispatch-sync [:grid-view/init component-id])
    (fn []
      [:<>
       [re-c/overview-proj-details-menu]
       [:div "project: " (:name @_project)]
       #_[:div "project: " (str (last (get (vec (:projects @model)) (:project @cross))))]
       ;[:pre (with-out-str (pprint @model))]
       ;[:pre (with-out-str (pprint @project))]

       [grid-view/grid-form
        component-id
        _project
        (e/keydown-rules component-id _project)]])))


