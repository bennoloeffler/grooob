(ns grooob.project-single-view.events
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rd]
            [tick.core :as t]
            [java.time :refer [LocalDate]]
            [goog.events.KeyCodes :as keycodes]
            [goog.events :as events]
            [goog.object :as gobj]
            [goog.functions]
            [belib.core :as bc :refer [pp]]
            [belib.browser :as bb]
            [belib.spec :as bs]
            [grooob.model.model-malli :as model-malli]
            [grooob.model.model-spec :as ms]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [grooob.project-single-view.core :as c]
            [grooob.time-transit])

  (:import goog.History
           [goog.events EventType KeyHandler]))


(defn keydown-rules [component-id _model]
  {:event-keys [
                ; MISC

                [[:model/save]
                 ;; will be triggered if
                 [{:keyCode keycodes/S :ctrlKey true}]]

                [[:common/navigate! :projects-portfolio nil nil]
                 ;; will be triggered if
                 [{:keyCode keycodes/P}]]

                ;; Move TASK with shift <- -> AD

                [[:project/task-move-up-down component-id _model -1]
                 ;; will be triggered if
                 [{:keyCode keycodes/UP :shiftKey true}]
                 [{:keyCode keycodes/W :shiftKey true}]]

                [[:project/task-move-up-down component-id _model 1]
                 ;; will be triggered if
                 [{:keyCode keycodes/DOWN :shiftKey true}]
                 [{:keyCode keycodes/S :shiftKey true}]]


                [[:project/task-move component-id _model 1]
                 ;; will be triggered if
                 [{:keyCode keycodes/RIGHT :shiftKey true}]
                 [{:keyCode keycodes/D :shiftKey true}]] ;  AD

                [[:project/task-move component-id _model -1]
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
                 [{:keyCode keycodes/Z}]]]



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
;; is pressed

(rf/reg-sub
  :project/cross
  (fn [db [_ component-id]]
    (let [component-key (keyword component-id)]
      (->> db :view component-key :cross))))

(rf/reg-event-fx
  :project/task-move
  (fn [cofx [_ component-id _model x]]
    (let [db           (:db cofx)
          ;; TODO this :projects-overview-form s path is HARDCODED!
          pr-cross     (get-in db [:view :projects-overview-form :cross])
          pr-idx       (:y pr-cross)

          comp-key     (keyword component-id)
          ta-cross     (get-in db [:view comp-key :cross])
          cross-x      (:x ta-cross)
          ta-idx       (:y ta-cross)
          size-x       (- (:max-cw @_model)
                          (:min-cw @_model))
          in-box       (not (or (>= (+ cross-x x) size-x)
                                (<= (+ cross-x x) 0)))
          update-cross (fn [model value]
                         (if in-box
                           (update-in model [:view comp-key :cross :x] + value)
                           model))]
      {:db       (-> db
                     (update :model model-malli/move-task pr-idx ta-idx (* 7 x))
                     (update-cross x))
       :dispatch [:grid-view/cross-visible]})))

(rf/reg-event-fx
  :project/task-move-up-down
  (fn [cofx [_ component-id _model y]]
    (let [db           (:db cofx)
          comp-key     (keyword component-id)
          ta-cross     (get-in db [:view comp-key :cross])
          ta-idx       (:y ta-cross)

          pr-cross     (get-in db [:view :projects-overview-form :cross])
          pr-idx       (:y pr-cross)

          size-y       (count (get-in db [:model :projects pr-idx :tasks]))
          in-box       (and (< (+ ta-idx y) size-y)
                            (>= (+ ta-idx y) 0))
          update-cross (fn [model value]
                         (if in-box
                           (update-in model [:view comp-key :cross :y] + value)
                           model))
          new-ta-idx   (if in-box (+ ta-idx y) ta-idx)]
      {:db       (-> db
                     (update-in [:model :projects pr-idx :tasks] bc/swap ta-idx new-ta-idx)
                     (update-cross y))
       :dispatch [:grid-view/cross-visible]})))
