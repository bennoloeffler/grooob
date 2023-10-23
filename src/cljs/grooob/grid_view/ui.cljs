(ns grooob.grid-view.ui
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
            [belib.date-time :as bd]
            [grooob.utils :as utils]
            [grooob.model.model-spec :as ms]
    ;[belib.cal-week-year :as bc]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [grooob.grid-view.events :as e])

  (:import goog.History
           [goog.events EventType KeyHandler]))

(def debounce-scroll-fn
  (bb/debounced-now #_goog.functions.debounce
    (fn dispatch-browser-scroll [event]
      ;(cljs.pprint/pprint (bb/js-obj->clj-map event))
      (rf/dispatch [:grid-view/browser-scroll]))
    100))

(defn scroll-fn [event]
  ;(println "scroll" event)
  (debounce-scroll-fn event))

(defn get-svg-x-offset [component-id]
  (let [svg-offset-x 0
        div          (.getElementById js/document component-id #_"divContainer")
        svg          (.getElementById js/document "svgElement")
        ;body         (.-body js/document)
        bcr-svg      (if svg (.getBoundingClientRect svg) svg-offset-x)
        bcr-div      (if div (.getBoundingClientRect div) svg-offset-x)
        svg-offset-x (- (.-x bcr-div) (.-x bcr-svg))
        svg-offset-x (if (js/isNaN svg-offset-x) 0 svg-offset-x)]
    svg-offset-x))

(defn cursor [component-id _model _grid]
  (let [_cross (rf/subscribe [:sub/data-path [:view (keyword component-id) :cross]])]
    (fn [component-id _model _grid]
      (let [g           @_grid
            c           @_cross
            x           (:x c)
            y           (:y c)
            ;_           (println "y: " y)
            ;_           (println (:projects @model))
            p-indicator (:name ((vec (:projects @_model)) y))
            ;_           (println p-indicator)
            cursor-week (bd/week-year-from-epoch-week (+ x 1 (:min-cw @_model)))]

        [:<>
         [:rect {:x            (* g x)
                 :y            (* g y)
                 :width        g
                 :height       g
                 :stroke       "black"
                 :stroke-width 2
                 :fill         "white"
                 :fill-opacity 0.2}]
         [:text.svg-non-select
          {:x                 (+ (* x g) (/ g 2))
           :y                 (+ (* g (+ y 2)))
           :fill              "grey"
           :font-family       "Nunito" #_"verdana" #_"arial" #_"titillium web"
           :font-weight       "bold"
           :dominant-baseline "middle"
           :font-size         (* 0.8 g)
           :writing-mode      "tb"
           :fill-opacity      0.5}

          (first (bd/weeks-indicators [cursor-week]))]
         [:text.svg-non-select
          {:x                 (* g (+ 2 x))
           :y                 (* g (+ y (/ 1 2)))
           :fill              "grey"
           ;:font-family       "Nunito" #_"verdana" #_"arial" #_"titillium web"
           ;:text-rendering    "geometricPrecision"
           :font-weight       "bold"
           :dominant-baseline "middle"
           :font-size         (* 0.8 g)
           :fill-opacity      0.5}
          (str p-indicator)]
         [:rect#cursor {:x            (- (* g x) g)
                        :y            (- (* g y) g)
                        :width        (* 3 g)
                        :height       (* 3 g)
                        :stroke       "white"
                        :stroke-width 0
                        :fill         "white"
                        :fill-opacity 0.0}]]))))


(defn cross [component-id _model _grid]
  (let [_cross (rf/subscribe [:sub/data-path [:view (keyword component-id) :cross]])]
    (fn [component-id _model _grid]

      (let [g      @_grid
            c      @_cross
            ;_      (println "draw: " #_(bb/js-obj->clj-map crossi))
            g-half (/ g 2)
            x      (:x c)
            y      (:y c)
            size-x (* g (- (:max-cw @_model) (:min-cw @_model)))
            size-y (* g (+ 5 (count (:projects @_model))))]
        [:<>
         [:line {:x1             (+ g-half (* g x))
                 :y1             0
                 :x2             (+ g-half (* g x))
                 :y2             size-y
                 :stroke         "white"
                 :stroke-width   (/ g 2)
                 :stroke-opacity 0.3}]
         [:line {:x1             0
                 :y1             (+ g-half (* g y))
                 :x2             size-x
                 :y2             (+ g-half (* g y))
                 :stroke         "white"
                 :stroke-width   (/ g 2)
                 :stroke-opacity 0.3}]]))))

(defn square [component-id _cross x y]
  (let [_grid (rf/subscribe [:sub/data-path [:view (keyword component-id) :grid]])]
    ;_cross (rf/subscribe [:sub/data-path [:view (keyword component-id) :cross]])]
    (fn [component-id _cross x y]
      (let [g   @_grid
            yg  (* (:y @_cross) g)
            sel (== yg y)
            col (if sel "black" "white")]
        [:rect {:x            x
                :y            y
                :width        g
                :height       g
                :stroke       "white"
                :stroke-width 1
                :fill         col
                :fill-opacity 0.2}]))))

; TODO split names and square?
(defn one-y-line [component-id _cross row start-cw len-cw name id]
  (let [grid           (rf/subscribe [:sub/data-path [:view (keyword component-id) :grid]])
        browser-scroll (rf/subscribe [:grid-view/browser-scroll])]
    ;_cross         (rf/subscribe [:sub/data-path [:view (keyword component-id) :cross]])]
    (fn [component-id _cross row start-cw len-cw name id]
      (let [gx (* start-cw @grid)
            gy (* row @grid)]
        (vec (cons :<> (conj (vec (map (fn [cw] [square component-id _cross (+ gx (* cw @grid)) gy])
                                       (range len-cw)))
                             [:text.svg-non-select
                              {:x                 (+ (get-svg-x-offset component-id) (* 2 @grid))
                               :y                 (+ (/ @grid 2) gy)
                               :fill              "black"
                               ;:font-weight       "bold"
                               :dominant-baseline "middle"
                               :font-size         (* 0.8 @grid)
                               :dummy             @browser-scroll} ; just to update the :x by (get-svg-x-offset)
                              (str name)])))))))

#_(defn map-one-y-line [component-id _cross start-x-total row {:keys [start-x len-x name id]}]
    [one-y-line component-id _cross row (- start-x start-x-total) len-x name id])

(defn y-axis [component-id _model]
  (let [_cross (rf/subscribe [:sub/data-path [:view (keyword component-id) :cross]])]
    (fn [component-id _model] (let [start-x-total (:min-cw @_model)
                                    sorted-model  @_model #_(update @_model :projects model/sorted-map-by-key)
                                    projects-html (vec (cons :<> (map-indexed
                                                                   (fn [row {:keys [start-x len-x name id]}]
                                                                     [one-y-line
                                                                      component-id
                                                                      _cross
                                                                      row
                                                                      (- start-x start-x-total)
                                                                      len-x
                                                                      name
                                                                      id])
                                                                   #_(partial map-one-y-line component-id _cross start-x-total)
                                                                   (:projects sorted-model))))]
                                projects-html))))

(comment
  ;(def part-fn (partial))
  (map-indexed (fn [idx e] [idx e]) ["a" "b"]))

(defn one-x-axis-element [text idx-x grid y]
  [:text.svg-non-select {:x                 (+ (* idx-x grid) (/ grid 2))
                         :y                 (+ y grid)
                         :fill              "black"
                         ;:font-weight       "bold"
                         :dominant-baseline "middle"
                         :font-size         (* 0.8 grid)
                         :writing-mode      "tb"}

   (str text)])

(defn x-axis [_model _grid]
  (let [m @_model
        g @_grid
        x (- (:max-cw m) (:min-cw m))
        p (:projects m)
        i (bd/weeks-indicators (bd/weeks-col-from-epoch-weeks (:min-cw m) x))
        y (* g (count p))]
    (vec (cons :<> (vec (map-indexed (fn [idx e] [one-x-axis-element e idx g y]) i))))
    #_(vec (cons :<> (conj (vec (map (fn [cw] [square (+ gx (* cw @grid)) gy])
                                     (range len-cw)))
                           [:text {:x                 (+ 11 (get-svg-x-offset))
                                   :y                 (+ (/ @grid 2) gy)
                                   :fill              "black"
                                   :font-weight       "bold"
                                   :dominant-baseline "middle"
                                   :font-size         (* 0.9 @grid)
                                   :dummy             @browser-scroll} ; just to update the :x by (get-svg-x-offset)
                            (str project-id)])))))


(defn t
  "this is a debugging helper to draw text"
  [svg-offset-x svg-offset-y & text-vals]
  (let [pairs (partition 2 text-vals)]
    (cons :<> (map (fn [pair y] (let [text (first pair)
                                      val  (second pair)]
                                  ^{:key y} [:text {:x    (+ svg-offset-x 100)
                                                    :y    (+ svg-offset-y y)
                                                    :fill "darkred"}
                                             ;:textantialiasing "true"
                                             ;:stroke "darkred"}
                                             (str text ": " val)]))
                   pairs
                   (range 20 1000 30)))))


(defn render-grid-form [component-id _model key-down-rules]
  (let [;browser-size   (rf/subscribe [:view/browser-size])
        ;browser-scroll (rf/subscribe [:view/browser-scroll])
        _grid (rf/subscribe [:sub/data-path [:view (keyword component-id) :grid]])]
    (fn []

      (let [

            ;svg-offset-x   100
            ;div            (.getElementById js/document "divContainer")
            ;svg            (.getElementById js/document "svgElement")
            ;body           (.-body js/document)
            ;_ (.log js/console svg)
            ;bcr-body       (if body (.getBoundingClientRect body) svg-offset-x)
            ;bcr-svg        (if svg (.getBoundingClientRect svg) svg-offset-x)
            ;bcr-div        (if div (.getBoundingClientRect div) svg-offset-x)
            ;doc-offset     (.-scrollTop (.-documentElement js/document))
            ;_              (b/prjs bcr-div)
            ;div-vis-width  (quot (.-width bcr-div) @grid)
            ;div-vis-height (quot (- (.-innerHeight js/window) (if (> (.-top bcr-div) 0) (.-top bcr-div) 0)) @grid)

            ;svg-offset-x   (- (.-x bcr-div) (.-x bcr-svg))
            ;svg-offset-y   (- (.-y bcr-div) (.-y bcr-svg)) #_(.-scrollTop (.-documentElement js/document)) #_(- (.-y bcr-body) (.-y bcr-svg))
            ;svg-offset-x   (get-svg-x-offset) #_(if (js/isNaN svg-offset-x) 0 svg-offset-x)
            ;svg-offset-y   (if (js/isNaN svg-offset-y) 0 svg-offset-y)
            ;div-offset-y   (.-y bcr-div)
            ;dist-div-top   (+ div-offset-y doc-offset)
            x-px (* @_grid (- (:max-cw @_model) (:min-cw @_model)) #_"100vw")
            y-px (* @_grid (+ (count (:projects @_model)) 5 #_"100vh")) ; 5 grids for
            ;xy-ratio       (/ x-px y-px)
            ;cx             (* @grid (:cw @cross-data))
            ;cy             (* @grid (:project @cross-data))
            ;_ (println cx " " cy)
            #__ #_(when (and div (> cx y-px))
                    (.scrollTo div cx cy))]
        ;{:keys [x y]} @size
        ;{:keys [jsw-inner-width jsw-inner-height]} @browser-size]
        ;(println " " x " " y " " jsw-inner-width " " jsw-inner-height)
        [:<>
         #_[:div #_:div#this-special-div
            [:pre "ID: " component-id "\n----\n" (with-out-str (pprint @_model))]]
         [:div
          {:id    component-id #_"divContainer"
           :style {:background "white"
                   ;:width (or (- jsw-inner-width 120) x)
                   :height     y-px #_(or (- jsw-inner-height 400) y)
                   ;:autoflow :off
                   :overflow-y :hidden}}
          ;:autoflow :auto}}


          [:svg {:id          "svgElement"
                 :style       {:background-color "lightgray"}
                 ;+:viewBox    (str "0 0 " 1000 " " (/ 1000 xy-ratio))
                 :width       x-px #_"100vw"
                 :height      y-px #_"100vh"
                 :font-family "Nunito"
                 :on-click    (partial e/click-fn component-id _model)}
           [cross component-id _model _grid]
           [y-axis component-id _model]
           [x-axis _model _grid]
           [cursor component-id _model _grid]
           [:circle {:cx           0 :cy 0 :r 25
                     :stroke       "white"
                     :stroke-width 1
                     :fill-opacity 0.7
                     :fill         utils/primary-color}]
           #_(t svg-offset-x (+ doc-offset)
                "grid" @_grid
                "cross" @_cross-data
                #_"cross-visible" #_(cross-visible @cross-data
                                                   (quot svg-offset-x @_grid)
                                                   (quot (- doc-offset dist-div-top) @_grid)
                                                   div-vis-width
                                                   div-vis-height)
                ;"div-offset-y" (quot div-offset-y @grid)
                ;"svg-offset-x" (quot svg-offset-x @grid)
                ;"svg-offset-y" (quot svg-offset-y @grid)
                ;"div-vis-width" div-vis-width
                ;"div-vis-height" div-vis-height
                ;"dist-div-top" (quot dist-div-top @grid)
                ;"doc-offset" (quot doc-offset @grid)

                #_"browser-scroll - just for update: " #_@browser-scroll)
           [:circle {:cx           x-px :cy y-px :r 25
                     :stroke       "white"
                     :stroke-width 1
                     :fill-opacity 0.7
                     :fill         utils/primary-color}]]]]))))

(defn grid-form [component-id
                 _model
                 key-down-rules]

  (if (or (not @_model) (= (:projects @_model) []))
    [:div (str "no model for component: " component-id)]
    (let [scroll-listener (atom nil)
          m-up-listener   (atom nil)
          m-down-listener (atom nil)
          m-move-listener (atom nil)]
      (reagent/create-class
        {:display-name           component-id
         :reagent-render         render-grid-form
         :component-did-mount
         (fn []
           ;(println "View mounted for " component-id)
           (rf/dispatch [::rp/set-keydown-rules
                         key-down-rules])

           (reset! scroll-listener
                   (events/listen (.getElementById js/document component-id)
                                  EventType.SCROLL scroll-fn))
           (reset! m-up-listener
                   (events/listen js/window
                                  EventType.MOUSEUP
                                  #(reset! e/start-dragging nil)))
           (reset! m-down-listener
                   (events/listen js/window
                                  EventType.MOUSEDOWN
                                  (fn [event]
                                    (reset! e/start-dragging
                                            {:x (.-offsetX event)
                                             :y (.-offsetY event)}))))
           (reset! m-move-listener
                   (events/listen js/window
                                  EventType.MOUSEMOVE
                                  (fn [event]
                                    (when @e/start-dragging
                                      (e/dragging-debounced
                                        @e/start-dragging
                                        {:x (.-offsetX event)
                                         :y (.-offsetY event)}))))))
         :component-will-unmount (fn []
                                   ;(println "View will unmount: " component-id)
                                   (rf/dispatch [::rp/set-keydown-rules
                                                 {}])
                                   (events/unlistenByKey @scroll-listener)
                                   (events/unlistenByKey @m-up-listener)
                                   (events/unlistenByKey @m-down-listener)
                                   (events/unlistenByKey @m-move-listener))}))))

