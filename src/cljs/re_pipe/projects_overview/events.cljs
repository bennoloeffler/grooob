(ns re-pipe.projects-overview.events
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
            [re-pipe.model :as model]
            [re-pipe.model-spec :as ms]
            [re-pipe.utils :as utils]
            [belib.cal-week-year :as bc]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [re-pipe.project-single-view.ui :as psv])

  (:import goog.History
           [goog.events EventType KeyHandler]))

(defn project-view-keydown-rules [component-id _model]
  {:event-keys [

                ;; Move PROJECT with shift <- -> AD

                [[:overview/project-move component-id 1]
                 ;; will be triggered if
                 [{:keyCode keycodes/RIGHT :shiftKey true}]
                 [{:keyCode keycodes/D :shiftKey true}]] ;  AD

                [[:overview/project-move component-id -1]
                 ;; will be triggered if
                 [{:keyCode keycodes/LEFT :shiftKey true}]
                 [{:keyCode keycodes/A :shiftKey true}]] ;  AD

                ;; Move CURSOR / CROSS with <- ->

                [;; this event
                 [:grid-view/cross-move component-id _model {:x 0 :y -1}]
                 ;; will be triggered if
                 [{:keyCode keycodes/UP}]
                 ;; or
                 [{:keyCode keycodes/W}]] ;  WASD
                ;; is pressed

                [[:grid-view/cross-move component-id _model {:x 0 :y 1}]
                 ;; will be triggered if
                 [{:keyCode keycodes/DOWN}]
                 [{:keyCode keycodes/S}]] ;  WASD

                [[:grid-view/cross-move component-id _model {:x 1 :y 0}]
                 ;; will be triggered if
                 [{:keyCode keycodes/RIGHT}]
                 [{:keyCode keycodes/D}]] ;  WASD

                [[:grid-view/cross-move component-id _model {:x -1 :y 0}]
                 ;; will be triggered if
                 [{:keyCode keycodes/LEFT}]
                 [{:keyCode keycodes/A}]] ;  WASD

                ;; ZOOM with  + / -  or with  z / shift-z

                [[:grid-view/zoom component-id 0.9]
                 ;; will be triggered if
                 [{:keyCode keycodes/DASH}]
                 [{:keyCode keycodes/Z :shiftKey true}]]

                [[:grid-view/zoom component-id 1.1]
                 ;; will be triggered if
                 [{:keyCode #_keycodes/PLUS_SIGN 187}] ; PLUS_SIGN does not work?
                 [{:keyCode keycodes/Z}]]

                [[:common/navigate! :project nil nil]
                 ;; will be triggered if
                 [{:keyCode keycodes/P}]]]



   ;; tab is pressed twice in a row
   ;[{:keyCode 9} {:keyCode 9}]]]


   ;; takes a collection of key combos that, if pressed, will clear
   ;; the recorded keys
   :clear-keys
   ;; will clear the previously recorded keys if
   [] #_[;; escape
         [{:keyCode 27}]
         ;; or Ctrl+g
         [{:keyCode 71
           :ctrlKey true}]]
   ;; is pressed

   ;; takes a collection of keys that will always be recorded
   ;; (regardless if the user is typing in an input, select, or textarea)
   :always-listen-keys
   ;; will always record if
   [;; enter
    {:keyCode 13}]
   ;; is pressed

   ;; takes a collection of keys that will prevent the default browser
   ;; action when any of those keys are pressed
   ;; (note: this is only available to keydown)
   :prevent-default-keys
   ;; will prevent the browser default action if
   [{:keyCode keycodes/UP}
    {:keyCode keycodes/DOWN}
    {:keyCode keycodes/LEFT}
    {:keyCode keycodes/RIGHT}
    ;; Ctrl+g
    #_{:keyCode 71
       :ctrlKey true}]})
(rf/reg-sub
  :model/model
  (fn [db _]
    (model/view-model (:model db))))


(rf/reg-sub
  :model/current-project
  (fn [db [_ component-id]]
    (let [comp-key            (keyword component-id)
          current-project-key (-> (:model db)
                                  :g/projects
                                  keys
                                  vec
                                  (get (-> db :view comp-key :cross :y)))
          current-project     (-> (:model db)
                                  :g/projects
                                  (get current-project-key))]
      (model/current-project current-project))))


(rf/reg-event-fx
  :overview/project-move
  (fn [cofx [_ component-id x]]
    (let [comp-key (keyword component-id)
          db       (:db cofx)
          cross    (get-in db [:view comp-key :cross])
          pr-keys  (vec (keys (get-in db [:model :g/projects])))
          pr-id    (pr-keys (:y cross))]
      {:db       (-> db
                     (update :model model/move-project pr-id (* 7 x))
                     (update-in [:view comp-key :cross :x] + x))
       :dispatch [:grid-view/cross-visible]})))

