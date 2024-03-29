(ns grooob.project-single-view.ui
  (:require
    [belib.core :as b]
    [re-frame.core :as rf]
    [grooob.comps.ui :as cui]
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
    [ajax.core :as ajax]
    [goog.history.EventType :as HistoryEventType]
    [time-literals.read-write]
    [luminus-transit.time :as time]
    [cognitect.transit :as transit]
    [re-pressed.core :as rp]
    [grooob.utils :as utils]
    [grooob.project-single-view.events :as e]
    ;[grooob.project-single-view.core :as c]
    [cuerdas.core :as str])



  (:import goog.History
           [goog.events EventType KeyHandler]))


; TODO
; scrolling at component-id



#_(defn test-params [p]
    (let [x (str p "--" p)]
      (fn [] [:div (str "params: " x)])))
(defn single-view-only [component-id]
  (let [_project (rf/subscribe [:model/current-project "projects-overview-form"])]
    (rf/dispatch-sync [:grid-view/init component-id])
    (fn []
      [grid-view/grid-form
       component-id
       _project
       (e/keydown-rules component-id _project)])))

(defn project-single-view [component-id]
  (let [;; TODO this :projects-overview-form s path is HARDCODED!
        _project (rf/subscribe [:model/current-project "projects-overview-form"])]
    (fn []
      [:<>
       [cui/overview-proj-details-menu]
       [:div "project: " (:name @_project)]
       #_[:div "project: " (str (last (get (vec (:projects @model)) (:project @cross))))]
       ;[:pre (with-out-str (pprint @model))]
       ;[:pre (with-out-str (pprint @project))]
       [single-view-only component-id]
       #_[grid-view/grid-form
          component-id
          _project
          (e/keydown-rules component-id _project)]])))



