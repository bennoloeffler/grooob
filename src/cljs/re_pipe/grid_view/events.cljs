(ns re-pipe.grid-view.events
  (:require [goog.events :as events]
            [re-frame.core :as rf]
            [re-pipe.model :as model]
            [belib.browser :as bb])
  (:import [goog.events EventType]))


(defn scrollCursorVisible
  ; TODO: use the function cross-visible to make it work in every browser?
  "This only works in google chrome."
  []
  (let [cursor (.getElementById js/document "cursor")]
    (.scrollIntoView cursor (clj->js {:behavior "smooth", :block "nearest", :inline "nearest"}))))

(defn cross-visible
  "returns x and y of the cross outside the
  visible area. {:x -3} means, that the cross
  is inside the visible area in y axis but
  -3 outside x. So the cross is left of
  the visible area."
  [cross offset-x offset-y visible-x visible-y]
  (let [offset-y (if (< offset-y 0) 0 offset-y)]
    ;(println "ox" offset-x "oy" offset-y "vx" visible-x "vy" visible-y)
    (cond-> {}
            ; TODO change :cw to x and :project to :y
            (< (:cw cross) offset-x) (assoc :cw (- (:cw cross) offset-x))
            (> (:cw cross) (+ visible-x offset-x)) (assoc :cw (- (:cw cross) (+ visible-x offset-x)))
            (< (:project cross) offset-y) (assoc :project (- (:project cross) offset-y))
            (> (:project cross) (+ visible-y offset-y)) (assoc :project (- (:project cross) (+ visible-y offset-y))))))
(comment
  (cross-visible {:cw 0 :project 0} 0 0 10 10)
  (cross-visible {:cw 0 :project 0} 1 0 10 10)
  (cross-visible {:cw 0 :project 0} 1 1 10 10)
  (cross-visible {:cw 11 :project 11} 0 0 10 10)
  (cross-visible {:cw 5 :project 6} 2 2 1 3))



(rf/reg-event-db
  :grid-view/cross-visible
  (fn [db [_ data]]
    (scrollCursorVisible)))

(rf/reg-event-fx
  :grid-view/init
  (fn [cofx [_ component-id grid cross]]
    (let [db       (:db cofx)
          comp-key (keyword component-id)]
      {:db (if (-> db :view comp-key)
             db
             (-> db
                 (assoc-in [:view comp-key :cross] (or cross {:y 0 :x 0}))
                 (assoc-in [:view comp-key :grid] (or grid 30))))})))

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




(rf/reg-event-db
  :grid-view/cross-abs-move
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


(defn click-fn [component-id _model event]
  ;(println component-id)
  ;(println @_model)
  ;(bb/prjs event)
  (let [;e    (js->clj event)
        ;em   (bb/js-obj->clj-map event)
        {:keys [x y]} (get-x-y-vec event)]
    ;(println x " " y)
    (rf/dispatch [:grid-view/cross-abs-move component-id _model x y])))

(defn dragging [from to]
  (println [from :to to]))

(def dragging-debounced (bb/debounced-now dragging 200))

(def start-dragging (atom nil))

;; TODO set the listeners to the right div
#_(events/listen js/window
                 EventType.MOUSEDOWN
                 (fn [event] (reset! start-dragging {:x (.-offsetX event)
                                                     :y (.-offsetY event)})))
(defn mouse-down-fn [event] (reset! start-dragging (get-x-y-vec event)))
#_(events/listen js/window
                 EventType.MOUSEUP
                 #(reset! start-dragging nil))
(defn mouse-up-fn [event] (reset! start-dragging nil))

(defn mouse-move-fn [event] (when @start-dragging
                              (dragging-debounced @start-dragging (get-x-y-vec event))))





(rf/reg-event-db
  :grid-view/browser-scroll
  (fn [db [_]]
    (-> db
        (update-in [:view :browser-scroll] (fnil inc 0)))))

(rf/reg-sub
  :grid-view/browser-scroll
  (fn [db _]
    (-> db :view :browser-scroll)))

(rf/reg-event-db
  :grid-view/zoom
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
  :grid-view/cross-move
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