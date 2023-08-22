(ns re-pipe.re-comps.ui
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
            [belib.hiccup :as bh]
    ;[re-pipe.grid-view.ui :as grid-view]
            [re-pipe.model-spec :as ms]
            [re-pipe.utils :as utils]
            [belib.cal-week-year :as bc]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]))
;[re-pipe.projects-overview.events :as e]))

(defn overview-proj-details-menu []
  (let [_page (rf/subscribe [:common/page-id])]
    (fn []
      (println "page: " @_page)
      [:div.tabs.is-small.is-toggle.is-toggle-rounded
       [:ul
        [:li {:class (when (= @_page :projects-portfolio) :is-active)} #_(bh/cs (when (= @_page :projects-portfolio) :is-active))
         [:a {:on-click #(rf/dispatch [:common/navigate! :projects-portfolio nil nil])} "Overview"]]
        [:li {:class (when (= @_page :project) :is-active)}
         [:a {:on-click #(rf/dispatch [:common/navigate! :project nil nil])} "Project"]]
        [:li {:class (when (= @_page :project-details) :is-active)}
         [:a {:on-click #(rf/dispatch [:common/navigate! :project-details nil nil])} "Details"]]
        [:li [:a "Load"]]]])))
#_[:div "overview project details (p to toggle)"]
