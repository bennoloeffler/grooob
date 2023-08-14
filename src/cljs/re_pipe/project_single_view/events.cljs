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
            [clojure.pprint :refer [pprint]]
            [belib.browser :as bb]
            [belib.spec :as bs]
            [re-pipe.model :as model]
            [re-pipe.model-spec :as ms]
            [belib.cal-week-year :as bc]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [re-pipe.project-single-view.core :as c])

  (:import goog.History
           [goog.events EventType KeyHandler]))


(defn keydown-rules [component-id _model]
  {:event-keys [

                ;; Move TASK with shift <- -> AD

                [[:project/task-move component-id 1]
                 ;; will be triggered if
                 [{:keyCode keycodes/RIGHT :shiftKey true}]
                 [{:keyCode keycodes/D :shiftKey true}]] ;  AD

                [[:project/task-move component-id -1]
                 ;; will be triggered if
                 [{:keyCode keycodes/LEFT :shiftKey true}]
                 [{:keyCode keycodes/A :shiftKey true}]] ;  AD

                ;; Move CURSOR / CROSS with <- ->

                [;; this event
                 [:project/cross-move component-id _model {:x 0 :y -1}]
                 ;; will be triggered if
                 [{:keyCode keycodes/UP}]
                 ;; or
                 [{:keyCode keycodes/W}]] ;  WASD
                ;; is pressed

                [[:project/cross-move component-id _model {:x 0 :y 1}]
                 ;; will be triggered if
                 [{:keyCode keycodes/DOWN}]
                 [{:keyCode keycodes/S}]] ;  WASD

                [[:project/cross-move component-id _model {:x 1 :y 0}]
                 ;; will be triggered if
                 [{:keyCode keycodes/RIGHT}]
                 [{:keyCode keycodes/D}]] ;  WASD

                [[:project/cross-move component-id _model {:x -1 :y 0}]
                 ;; will be triggered if
                 [{:keyCode keycodes/LEFT}]
                 [{:keyCode keycodes/A}]] ;  WASD

                ;; ZOOM with  + / -  or with  z / shift-z

                [[:project/zoom component-id 0.9]
                 ;; will be triggered if
                 [{:keyCode keycodes/DASH}]
                 [{:keyCode keycodes/Z :shiftKey true}]]

                [[:project/zoom component-id 1.1]
                 ;; will be triggered if
                 [{:keyCode #_keycodes/PLUS_SIGN 187}] ; PLUS_SIGN does not work?
                 [{:keyCode keycodes/Z}]]

                [[:common/navigate! :projects-portfolio nil nil]
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
;; is pressed


(defn get-x-y-vec [event]
  (let [doc-offset (.-scrollTop (.-documentElement js/document))
        xe         (.-pageX event)
        ye         (.-pageY event)
        svg        (.getElementById js/document "svgElement")
        rect       (.getBoundingClientRect svg)
        left       (.-left rect)
        top        (.-top rect)
        x          (- xe left)
        y          (- ye top doc-offset)]
    {:x x :y y}))

(defn click-fn [component-id _model event]
  ;(println component-id)
  ;(println @_model)
  ;(bb/prjs event)
  (let [;e    (js->clj event)
        ;em   (bb/js-obj->clj-map event)
        {:keys [x y]} (get-x-y-vec event)]
    ;(println x " " y)
    (rf/dispatch [:project/cross-abs-move component-id _model x y])))


(defn scrollCursorVisible []
  (let [cursor (.getElementById js/document "cursor")]
    (.scrollIntoView cursor (clj->js {:behavior "smooth", :block "nearest", :inline "nearest"}))))

(rf/reg-event-db
  :project/cross-visible
  (fn [db [_ data]]
    (scrollCursorVisible)))

(rf/reg-event-fx
  :project-view/init
  (fn [cofx [_ component-id]]
    (let [db       (:db cofx)
          comp-key (keyword component-id)]
      {:db (if (-> db :view comp-key)
             db
             (-> db
                 (assoc-in [:view comp-key :cross] {:y 0 :x 0})
                 (assoc-in [:view comp-key :grid] 40)))})))



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


(rf/reg-event-db
  :project/cross-abs-move
  (fn [db [_ component-id _model x y]]
    (let [comp-key (keyword component-id)
          g        (get-in db [:view comp-key :grid])
          xg       (quot x g)
          yg       (quot y g)
          model    @_model
          ; TODO set current task id
          size-y   (count (:projects model))]
      ;(println "x/y: " x "/" y ", xg/yg: " xg "/" yg ", size-y" size-y)
      (if (< yg size-y)
        (assoc-in db
                  [:view comp-key :cross]
                  {:x xg :y yg})
        db))))

(rf/reg-event-db
  :project/zoom
  (fn [db [_ component-id zoom-factor]]
    ;(println "zoom" zoom-factor)
    (let [component-key (keyword component-id)
          cross         (get-in db [:view component-key :cross])
          ;{sx :x sy :y} (get-in db [:view :size])
          g             (get-in db [:view component-key :grid])
          new-grid      (* zoom-factor g)
          valid-grid    (if (or (< new-grid 3)
                                (> new-grid 100))
                          g
                          new-grid)
          #_new-cw      #_(if (>= (* valid-grid (:cw cross)) sx)
                            (dec (long (quot sx valid-grid)))
                            (:cw cross))
          #_new-pr      #_(if (>= (* valid-grid (:project cross)) sy)
                            (dec (long (quot sy valid-grid)))
                            (:project cross))
          #_valid-cross #_{:cw new-cw :project new-pr}]
      (-> db
          (assoc-in [:view component-key :grid] valid-grid)
          #_(assoc-in [:view :cross] valid-cross)
          #_(update-in [:view :size :x] / zoom-factor)
          #_(update-in [:view :size :y] / zoom-factor)))))


(rf/reg-event-fx
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
       :dispatch [:project/cross-visible]})))


; TODO BUG work with pr-id and task-id
;      keys is not stable in sequence
;      between key presses
(rf/reg-event-fx
  :project/task-move
  (fn [cofx [_ component-id x]]
    (let [db            (:db cofx)
          pr-cross      (get-in db [:view :cross])
          pr-keys       (vec (keys (get-in db [:model :g/projects])))
          pr-id         (pr-keys (:project pr-cross))

          component-key (keyword component-id)
          ta-cross      (get-in db [:view component-key :cross])
          ta-keys       (vec (keys (model/sorted-map-by-key
                                     (get-in db [:model :g/projects pr-id :g/tasks])
                                     :g/sequence-num)))
          ta-id         (ta-keys (:y ta-cross))]
      (println "pr: " pr-id ", ta: " ta-id)
      {:db       (-> db
                     (update :model model/move-task pr-id ta-id (* 7 x))
                     (update-in [:view component-key :cross :x] + x))
       :dispatch [:project/cross-visible]})))
