(ns re-pipe.project-single-view.events
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rd]
            [tick.core :as t]
            [java.time :refer [LocalDate]]
            [goog.events.KeyCodes :as keycodes]
            [goog.events :as events]
            [goog.object :as gobj]
            [goog.functions]
    ;[clojure.pprint :refer [pprint]]
            [belib.core :as bc :refer [pprint]]
            [belib.browser :as bb]
            [belib.spec :as bs]
            [re-pipe.model :as model]
            [re-pipe.model-spec :as ms]
    ;[belib.cal-week-year :as bc]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [re-pipe.project-single-view.core :as c]
            [re-pipe.time-transit])

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







#_(defn get-svg-x-offset []
    (let [svg-offset-x 0
          div          (.getElementById js/document "divContainer")
          svg          (.getElementById js/document "svgElement")
          body         (.-body js/document)
          bcr-svg      (if svg (.getBoundingClientRect svg) svg-offset-x)
          bcr-div      (if div (.getBoundingClientRect div) svg-offset-x)
          svg-offset-x (- (.-x bcr-div) (.-x bcr-svg))
          svg-offset-x (if (js/isNaN svg-offset-x) 0 svg-offset-x)]
      svg-offset-x))

(rf/reg-sub
  :project/cross
  (fn [db [_ component-id]]
    (let [component-key (keyword component-id)]
      (->> db :view component-key :cross))))

(rf/reg-sub
  :sub/data-path
  (fn [db [_ path]]
    (get-in db path)))



#_(rf/reg-event-fx
    :project/cross-move
    ;:<- [:model/current-project]
    (fn [cofx [_ component-id _model data]]
      (let [component-key (keyword component-id)
            db            (:db cofx)
            new-cross     (merge-with +
                                      (get-in db [:view component-key :cross])
                                      data)
            ;g             (get-in db [:view component-key :grid])
            ;model        (get-in db [:view component-key :model])
            size-x        (- (:max-cw @_model)
                             (:min-cw @_model))
            size-y        (count (:projects @_model))
            ;_             (prn size-x size-y)
            ;_             (prn new-cross)
            valid-cross   (if (or (< (:x new-cross) 0)
                                  (< (:y new-cross) 0)
                                  (>= (:x new-cross) size-x)
                                  (>= (:y new-cross) size-y))
                            (get-in db [:view component-key :cross])
                            new-cross)]
        ;(println valid-cross)
        {:db       (assoc-in db [:view component-key :cross]
                             valid-cross)
         :dispatch [:grid-view/cross-visible]})))


(rf/reg-event-fx
  :project/task-move
  (fn [cofx [_ component-id _model x]]
    #_(println "move to: " x)
    #_(pprint @_model)
    (let [db           (:db cofx)
          ;; TODO this :projects-overview-form s path is HARDCODED!
          pr-cross     (get-in db [:view :projects-overview-form :cross])
          pr-keys      (vec (keys (bc/sorted-map-by-keys
                                    (get-in db [:model :g/projects])
                                    :g/sequence-num)))
          pr-id        (pr-keys (:y pr-cross))

          comp-key     (keyword component-id)
          ta-cross     (get-in db [:view comp-key :cross])
          cross-x      (:x ta-cross)
          ta-keys      (vec (keys (model/sort-tasks-map-by-res
                                    (get-in db [:model :g/projects pr-id :g/tasks])
                                    (model/resource-id-sequence (:model db)))))
          ta-id        (ta-keys (:y ta-cross))
          size-x       (- (:max-cw @_model)
                          (:min-cw @_model))
          in-box       (not (or (>= (+ cross-x x) size-x)
                                (<= (+ cross-x x) 0)))
          update-cross (fn [model value]
                         (if in-box
                           (update-in model [:view comp-key :cross :x] + value)
                           model))]
      ;(println "pr: " pr-id ", ta: " ta-id)
      {:db       (-> db
                     (update :model model/move-task pr-id ta-id (* 7 x))
                     (update-cross x))
       :dispatch [:grid-view/cross-visible]})))
