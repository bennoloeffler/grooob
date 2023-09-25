(ns re-pipe.project-details.events
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
            [re-pipe.model.model :as model]
            [re-pipe.model.model-spec :as ms]
            [re-pipe.utils :as utils]
            [belib.core :as bc]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [re-pipe.time-transit]))

(defn project-details-view-keydown-rules [component-id _model]
  {:event-keys [

                ;; MISC

                [[:model/save]
                 ;; will be triggered if
                 [{:keyCode keycodes/S :ctrlKey true}]]

                [[:common/navigate! :project nil nil]
                 ;; will be triggered if
                 [{:keyCode keycodes/P}]]]})


