(ns re-pipe.project-ui
  (:require [re-frame.core :as rf]
            [tick.core :as t]
            [goog.events.KeyCodes :as keycodes]
            [goog.events :as events]
            [goog.object :as gobj]
            [goog.functions]
            [clojure.pprint :refer [pprint]]
            [belib.browser :as b]
            [belib.spec :as bs]
            [re-pipe.model :as model]
            [re-pipe.model-spec :as ms]
            [belib.cal-week-year :as bc]
            [goog.history.EventType :as HistoryEventType])
  (:import goog.History
           [goog.events EventType KeyHandler]))

(bs/validate :g/model ms/example-model)

(defn scrollCursorVisible []
  (let [cursor (.getElementById js/document "cursor")]
    (.scrollIntoView cursor (clj->js {:behavior "smooth", :block "nearest", :inline "nearest"}))))

(defn get-svg-x-offset []
  (let [svg-offset-x 0
        div          (.getElementById js/document "divContainer")
        svg          (.getElementById js/document "svgElement")
        body         (.-body js/document)
        bcr-svg      (if svg (.getBoundingClientRect svg) svg-offset-x)
        bcr-div      (if div (.getBoundingClientRect div) svg-offset-x)
        svg-offset-x (- (.-x bcr-div) (.-x bcr-svg))
        svg-offset-x (if (js/isNaN svg-offset-x) 0 svg-offset-x)]
    svg-offset-x))


; the portfolio-view consists of a grid of calendar-weeks and projects
;      0   1  2  3  4
;      w1 w2 w3 w4 w5 ......
; 0   p1     |
; 1   p2-----xxxxxxxxxxxxxxx
; 2   p3     xxxxxxxxxxxxxxx
; 3   p4     xxxxxxxxxxxxxxx
;      .     xxxxxxxxxxxxxxx
;      .     xxxxxxxxxxxxxxx

; the portfolio-view has an offset offset-cw
; the portfolio-view has an offset offset-pr
; when e. g. offset-cw = 2 and offset-pr = 1
; then the view starts with cweek 2 and project 2

(defn cross-pos []
  (let [cross (rf/subscribe [:view/cross])
        model (rf/subscribe [:model/model])]
    (fn []

      (let [now (bc/week-year-from-abs-week (+ (:min-cw @model) (:cw @cross)))]
        [:pre (str now ", " @cross ", min-max-cw: " (:min-cw @model) ", " (:max-cw @model) ", now: " (bc/get-abs-current-week))]))))


(defn weeks-from-abs-weeks [start-week num-weeks]
  (vec (map (fn [e] (bc/week-year-from-abs-week (+ start-week e 1)))
            (range num-weeks))))

(defn weeks-indicators [all-year-weeks]
  (map #(str (first %) "-" (second %)) all-year-weeks))

(comment
  (weeks-from-abs-weeks 100 10)
  (weeks-indicators (weeks-from-abs-weeks 100 10)))

(defn week [indicator cw g y]
  [:text {:x                 (+ (* cw g) (/ g 2))
          :y                 (+ y g)
          :fill              "black"
          :font-weight       "bold"
          :dominant-baseline "middle"
          :font-size         (* 0.9 g)
          :writing-mode      "tb"}

   (str indicator)])

(defn weeks []
  (let [grid  (rf/subscribe [:view/grid])
        model (rf/subscribe [:model/model])]
    (fn []
      (let [m @model
            g @grid
            x (- (:max-cw m) (:min-cw m))
            p (:projects m)
            i (weeks-indicators (weeks-from-abs-weeks (:min-cw m) x))
            y (* g (count p))]
        (vec (cons :<> (vec (map-indexed (fn [idx e] [week e idx g y]) i))))
        #_(vec (cons :<> (conj (vec (map (fn [cw] [square (+ gx (* cw @grid)) gy])
                                         (range len-cw)))
                               [:text {:x                 (+ 11 (get-svg-x-offset))
                                       :y                 (+ (/ @grid 2) gy)
                                       :fill              "black"
                                       :font-weight       "bold"
                                       :dominant-baseline "middle"
                                       :font-size         (* 0.9 @grid)
                                       :dummy             @browser-scroll} ; just to update the :x by (get-svg-x-offset)
                                (str project-id)])))))))


(defn cursor []
  (let [cross (rf/subscribe [:view/cross])
        model (rf/subscribe [:model/model])
        grid  (rf/subscribe [:view/grid])]
    (fn []

      (let [g           @grid
            c           @cross
            x           (:cw c)
            y           (:project c)
            cursor-week (bc/week-year-from-abs-week (+ x 1 (:min-cw @model)))]

        [:<>
         [:rect {:x            (* g x)
                 :y            (* g y)
                 :width        g
                 :height       g
                 :stroke       "black"
                 :stroke-width 2
                 :fill         "white"
                 :fill-opacity 0.2}]
         [week (first (weeks-indicators [cursor-week])) x g (* g (+ 1 y))]
         [:rect#cursor {:x            (- (* g x) g)
                        :y            (- (* g y) g)
                        :width        (* 3 g)
                        :height       (* 3 g)
                        :stroke       "white"
                        :stroke-width 0
                        :fill         "white"
                        :fill-opacity 0.0}]]))))
(defn cross []
  (let [cross (rf/subscribe [:view/cross])
        model (rf/subscribe [:model/model])
        grid  (rf/subscribe [:view/grid])]
    (fn []

      (let [g      @grid
            c      @cross
            g-half (/ g 2)
            x      (:cw c)
            y      (:project c)
            size-x (* g (- (:max-cw @model) (:min-cw @model)))
            size-y (* g (+ 5 (count (:projects @model))))]
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


(defn square []
  (let [grid (rf/subscribe [:view/grid])]
    (fn [x y]
      (let [g @grid]
        [:rect {:x            x
                :y            y
                :width        g
                :height       g
                :stroke       "white"
                :stroke-width 1
                :fill         "black"
                :fill-opacity 0.1}]))))

(defn project []
  (let [grid           (rf/subscribe [:view/grid])
        browser-scroll (rf/subscribe [:view/browser-scroll])]
    (fn [row start-cw len-cw project-id]
      (let [gx (* start-cw @grid)
            gy (* row @grid)]
        (vec (cons :<> (conj (vec (map (fn [cw] [square (+ gx (* cw @grid)) gy])
                                       (range len-cw)))
                             [:text {:x                 (+ 11 (get-svg-x-offset))
                                     :y                 (+ (/ @grid 2) gy)
                                     :fill              "black"
                                     :font-weight       "bold"
                                     :dominant-baseline "middle"
                                     :font-size         (* 0.9 @grid)
                                     :dummy             @browser-scroll} ; just to update the :x by (get-svg-x-offset)
                              (str project-id)])))))))

#_[:text {:x    10
          :y    gy
          :fill "blue"}
   (str 123)]


(defn projects []
  (let [model (rf/subscribe [:model/model])]
    (fn []
      (let [start-cw      (:min-cw @model)
            projects-html (vec (cons :<> (map (fn [[idx start weeks project-id]]
                                                [project idx (- start start-cw) weeks project-id])
                                              (:projects @model))))]
        ;(pprint start-cw)
        ;(pprint projects-html)
        projects-html))))

(declare scroll-fn)

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

(defn t [svg-offset-x svg-offset-y & text-vals]
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

(defn grid+cross []
  (let [;size (rf/subscribe [:view/size])
        browser-size   (rf/subscribe [:view/browser-size])
        browser-scroll (rf/subscribe [:view/browser-scroll])
        grid           (rf/subscribe [:view/grid])
        ; TODO M
        m              (rf/subscribe [:model/model])
        cross-data     (rf/subscribe [:view/cross])
        registered     (atom nil)]

    (fn []
      (let [

            svg-offset-x   100
            div            (.getElementById js/document "divContainer")
            svg            (.getElementById js/document "svgElement")
            body           (.-body js/document)
            ; todo move to initialisation code
            _              (when div #_(and div (not @registered)) (events/listen div
                                                                                  EventType.SCROLL #_(-> KeyHandler .-EventType .-KEY)
                                                                                  scroll-fn)
                                                                   (reset! registered true))
            ;_ (.log js/console svg)
            bcr-body       (if body (.getBoundingClientRect body) svg-offset-x)
            bcr-svg        (if svg (.getBoundingClientRect svg) svg-offset-x)
            bcr-div        (if div (.getBoundingClientRect div) svg-offset-x)
            doc-offset     (.-scrollTop (.-documentElement js/document))
            ;_              (b/prjs bcr-div)
            div-vis-width  (quot (.-width bcr-div) @grid)
            div-vis-height (quot (- (.-innerHeight js/window) (if (> (.-top bcr-div) 0) (.-top bcr-div) 0)) @grid)

            svg-offset-x   (- (.-x bcr-div) (.-x bcr-svg))
            svg-offset-y   (- (.-y bcr-div) (.-y bcr-svg)) #_(.-scrollTop (.-documentElement js/document)) #_(- (.-y bcr-body) (.-y bcr-svg))
            svg-offset-x   (get-svg-x-offset) #_(if (js/isNaN svg-offset-x) 0 svg-offset-x)
            svg-offset-y   (if (js/isNaN svg-offset-y) 0 svg-offset-y)
            div-offset-y   (.-y bcr-div)
            dist-div-top   (+ div-offset-y doc-offset)
            x-px           (* @grid (- (:max-cw @m) (:min-cw @m)) #_"100vw")
            y-px           (* @grid (+ (count (:projects @m)) 5 #_"100vh")) ; 5 grids for
            xy-ratio       (/ x-px y-px)
            cx             (* @grid (:cw @cross-data))
            cy             (* @grid (:project @cross-data))
            ;_ (println cx " " cy)
            #__ #_(when (and div (> cx y-px))
                    (.scrollTo div cx cy))]
        ;{:keys [x y]} @size
        ;{:keys [jsw-inner-width jsw-inner-height]} @browser-size]
        ;(println " " x " " y " " jsw-inner-width " " jsw-inner-height)
        [:div#divContainer
         {:style
          {
           :background "white"
           ;:width (or (- jsw-inner-width 120) x)
           :height     y-px #_(or (- jsw-inner-height 400) y)
           ;:autoflow :off
           :overflow-y :hidden}}
         ;:autoflow :auto}}


         [:svg {:id     "svgElement"
                :style  {:background-color "lightgray"}
                ;+:viewBox    (str "0 0 " 1000 " " (/ 1000 xy-ratio))
                :width  x-px #_"100vw"
                :height y-px #_"100vh"}
          ;:onLoad     #(println "load svg")}
          [cross]
          [projects]
          [weeks]
          [cursor]
          [:circle {:cx 0 :cy 0 :r 5 :fill "red"}]
          (t svg-offset-x (+ doc-offset)
             ;"grid" @grid
             ;"cross" @cross-data
             #_"cross-visible" #_(cross-visible @cross-data
                                                (quot svg-offset-x @grid)
                                                (quot (- doc-offset dist-div-top) @grid)
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

          #_[:text {:x (+ svg-offset-x 100) :y (+ svg-offset-y 20) :fill "black " :stroke "black"}
             (str "grid: " @grid)]
          #_[:text {:x (+ svg-offset-x 100) :y (+ svg-offset-y 50) :fill "black " :stroke "black"}
             (str "width: " (* @grid (- (:max-cw @m) (:min-cw @m)) #_"100vw"))]
          #_[:text {:x (+ svg-offset-x 100) :y (+ svg-offset-y 80) :fill "black " :stroke "black"}
             (str "height: " (* @grid (count (:projects @m))))]
          #_[:text {:x (+ svg-offset-x 100) :y (+ svg-offset-y 110) :fill "black " :stroke "black"}
             (str "offset-x: " (quot svg-offset-x @grid))]
          #_[:text {:x (+ svg-offset-x 100) :y (+ svg-offset-y 140) :fill "black " :stroke "black"}
             (str "offset-y: " (quot svg-offset-y @grid))]
          #_[:text {:x (+ svg-offset-x 100) :y (+ svg-offset-y 170) :fill "black " :stroke "black"}
             (str "cross: " @cross-data ", scroll: " @browser-scroll)]
          #_[:text {:x (+ svg-offset-x 100) :y (+ svg-offset-y 200) :fill "black " :stroke "black"}
             (str "total height: " div-vis-height)]
          #_[:text {:x (+ svg-offset-x 100) :y (+ svg-offset-y 230) :fill "black " :stroke "black"}
             (str "cross-visible: " (cross-visible @cross-data
                                                   (quot svg-offset-x @grid)
                                                   (quot div-offset-y @grid)
                                                   div-vis-width
                                                   div-vis-height))]

          [:circle {:cx x-px :cy y-px :r 5 :fill "red"}]]]))))



(defn logged-in-form []
  [:<>
   [:br]
   [cross-pos]
   [grid+cross]
   #_[:pre (with-out-str (pprint @re-frame.db/app-db))]
   #_[:pre (str "logged in: " @(rf/subscribe [:user]))]])

(def grid 20)
(declare create-model)

(rf/reg-event-fx
  :view/init
  (fn [cofx _]
    {:db (-> (:db cofx)
             (assoc-in [:view :cross] {:project 0 :cw 0})
             (assoc-in [:view :size] {:x 2000 :y 1000})
             (assoc-in [:view :grid] grid)
             (assoc-in [:view :offset-pr] 0)
             (assoc-in [:view :offset-cw] 0)
             (assoc-in [:model] (create-model)))}))

(rf/reg-event-db
  :view/cross-visible
  (fn [db [_ data]]
    (scrollCursorVisible)))

(rf/reg-event-db
  :view/cross-move

  (fn [db [_ data]]
    (let [new-cross   (merge-with +
                                  (get-in db [:view :cross])
                                  data)
          g           (get-in db [:view :grid])
          ; TODO M
          model       (get-in db [:model])
          size-x      (* g (- (get (second (:g/start-end-model-cw model)) 0)
                              (get (first (:g/start-end-model-cw model)) 0)))
          size-y      (* g (count (:g/projects model)))
          valid-cross (if (or (< (:cw new-cross) 0)
                              (< (:project new-cross) 0)
                              (>= (* g (:cw new-cross)) size-x)
                              (>= (* g (:project new-cross)) size-y))
                        (get-in db [:view :cross])
                        new-cross)]
      (assoc-in db [:view :cross]
                valid-cross))))

(rf/reg-event-db
  :view/cross-abs-move
  (fn [db [_ x y]]
    (let [g  (get-in db [:view :grid])
          cw (quot x g)
          pr (quot y g)]
      ;(println "x/y: "x"/"y ", cw/pr: "cw"/"pr)
      (assoc-in db [:view :cross]
                {:cw cw :project pr}))))


(rf/reg-event-db
  :view/project-move
  (fn [db [_ cw]]
    (let [cross   (get-in db [:view :cross])
          pr-keys (vec (keys (get-in db [:model :g/projects])))
          pr-id   (pr-keys (:project cross))]
      (-> db

          ; TODO M
          (update :model model/move-project pr-id (* 7 cw))
          (update-in [:view :cross :cw] + cw)))))


(rf/reg-event-db
  :view/zoom
  (fn [db [_ zoom-factor]]
    (println "zoom" zoom-factor)
    (let [cross       (get-in db [:view :cross])
          {sx :x sy :y} (get-in db [:view :size])
          g           (get-in db [:view :grid])
          new-grid    (* zoom-factor g)
          valid-grid  (if (or (< new-grid 3)
                              (> new-grid 100))
                        g
                        new-grid)
          new-cw      (if (>= (* valid-grid (:cw cross)) sx)
                        (dec (long (quot sx valid-grid)))
                        (:cw cross))
          new-pr      (if (>= (* valid-grid (:project cross)) sy)
                        (dec (long (quot sy valid-grid)))
                        (:project cross))
          valid-cross {:cw new-cw :project new-pr}]
      (-> db
          (assoc-in [:view :grid] valid-grid)
          (assoc-in [:view :cross] valid-cross)
          #_(update-in [:view :size :x] / zoom-factor)
          #_(update-in [:view :size :y] / zoom-factor)))))

(defn get-browser-size []
  {:jsw-inner-width      (.-innerWidth js/window)
   :jsd-de-client-width  (.-clientWidth (.-documentElement js/document))
   :jsd-b-client-width   (.-clientWidth (.-body js/document))
   :jsw-inner-height     (.-innerHeight js/window)
   :jsd-de-client-height (.-clientHeight (.-documentElement js/document))
   :jsd-b-client-height  (.-clientHeight (.-body js/document))})

(rf/reg-event-db
  :view/resize
  (fn [db [_]]
    (-> db
        (update-in [:view :resize] (fnil inc 0))
        (assoc-in [:view :browser-size] (get-browser-size))
        #_(assoc-in [:view :cross] valid-cross)
        #_(update-in [:view :size :x] / zoom-factor)
        #_(update-in [:view :size :y] / zoom-factor))))

(rf/reg-event-db
  :view/browser-scroll
  (fn [db [_]]
    (-> db
        (update-in [:view :browser-scroll] (fnil inc 0)))))
;(assoc-in [:view :browser-size] (get-browser-size)))))



(def debounce-resize-fn
  (b/debounced-now #_goog.functions.debounce
    (fn resize-fn [event] (rf/dispatch [:view/resize])) 100))

(defn resize-fn [event]
  (debounce-resize-fn event)
  #_(goog.functions.debounce #(rf/dispatch [:view/resize]) 300)
  #_(rf/dispatch [:view/resize])
  ;(.log js/console event)
  #_(println (pr-str (js->clj event))))
;(let [event-map (js->clj event :keywordize-keys true)]
;(println "cljs data: " event-map))


(events/listen js/window
               EventType.RESIZE
               resize-fn)

(def debounce-scroll-fn
  (b/debounced-now #_goog.functions.debounce
    (fn dispatch-browser-scroll [event]
      ;(cljs.pprint/pprint (b/js-obj->clj-map event))
      (rf/dispatch [:view/browser-scroll]))
    400))

(defn scroll-fn [event]
  ;(println "scroll" event)
  (debounce-scroll-fn event)
  ;(cljs.pprint/pprint (b/js-obj->clj-map event))
  #_(goog.functions.debounce #(rf/dispatch [:view/browser-scroll]) 300)
  #_(rf/dispatch [:view/browser-scroll]))

(events/listen js/window
               EventType.SCROLL
               scroll-fn)


(rf/reg-sub
  :view/cross
  (fn [db _]
    (-> db :view :cross)))

; TODO make it a derived one
#_(rf/reg-sub
    :view/size
    (fn [db _]
      (-> db :view :size)))


(rf/reg-sub
  :view/browser-size
  (fn [db _]
    (-> db :view :browser-size)))

(rf/reg-sub
  :view/browser-scroll
  (fn [db _]
    (-> db :view :browser-scroll)))

; todo remove
(rf/reg-sub
  :view/resize
  (fn [db _]
    (-> db :view :resize)))

#_(rf/reg-sub
    :model/projects
    (fn [db _]
      (get-in db [:model :projects])))

(rf/reg-sub
  :model/model
  (fn [db _]
    (model/view-model (:model db))))

(rf/reg-sub
  :view/grid
  (fn [db _]
    (-> db :view :grid)))

(def shift-down (atom false))
(defn capture-key
  "Given a `keycode`, execute function `f` "
  ; see https://github.com/reagent-project/historian/blob/master/src/cljs/historian/keys.cljs
  [keycode-map]
  (let [;key-handler (KeyHandler. js/document)
        press-fn (fn [key-press]

                   (when (= 16 (.. key-press -keyCode))
                     (reset! shift-down true))
                   (when-let [f (get keycode-map (.. key-press -keyCode))]
                     (.preventDefault key-press)
                     (f)))]

    ;(println (.. key-press -keyCode)))))]
    (println "register listeners")
    (events/listen js/window
                   EventType.KEYDOWN #_(-> KeyHandler .-EventType .-KEY)
                   press-fn)
    (events/listen js/window
                   EventType.KEYUP #_(-> KeyHandler .-EventType .-KEY)
                   (fn [key-press]
                     (when (= 16 (.. key-press -keyCode))
                       (reset! shift-down false))))))


(defn mouse-fn [event]
  ;(b/prjs event)
  (let [x (.-offsetX event)
        y (.-offsetY event)]
    (rf/dispatch [:view/cross-abs-move x y])))

(events/listen js/window
               EventType.CLICK
               mouse-fn)


(defn dragging [from to]
  (pprint [from :to to]))
(def dragging-debounced (b/debounced-now dragging 200))

(def start-dragging (atom nil))

(events/listen js/window
               EventType.MOUSEDOWN
               (fn [event] (reset! start-dragging {:x (.-offsetX event)
                                                   :y (.-offsetY event)})))
(events/listen js/window
               EventType.MOUSEUP
               #(reset! start-dragging nil))

(events/listen js/window
               EventType.MOUSEMOVE
               (fn [event] (when @start-dragging
                             (dragging-debounced @start-dragging {:x (.-offsetX event)
                                                                  :y (.-offsetY event)}))))


(defn register-key-handler []
  ;; sets up the event listener
  (capture-key {;keycodes/ENTER           #(println "ENTER")
                #_keycodes/PLUS_SIGN 187 #(do (rf/dispatch [:view/zoom 1.1])
                                              #_(rf/dispatch [:view/cross-visible]))
                #_keycodes/SEMICOLON 189 #(do (rf/dispatch [:view/zoom 0.9])
                                              #_(rf/dispatch [:view/cross-visible]))
                keycodes/LEFT            #(if @shift-down
                                            (rf/dispatch [:view/project-move -1])
                                            (do (rf/dispatch [:view/cross-move {:cw (- 1) :project 0}])
                                                (rf/dispatch [:view/cross-visible])))
                keycodes/RIGHT           #(if @shift-down
                                            (rf/dispatch [:view/project-move 1])
                                            (do (rf/dispatch [:view/cross-move {:cw 1 :project 0}])
                                                (rf/dispatch [:view/cross-visible])))
                keycodes/UP              #(do (rf/dispatch [:view/cross-move {:cw 0 :project (- 1)}])
                                              (rf/dispatch [:view/cross-visible]))
                keycodes/DOWN            #(do (rf/dispatch [:view/cross-move {:cw 0 :project 1}])
                                              (rf/dispatch [:view/cross-visible]))}))


(def p-small [[1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]])

(def p-medium [[3 2 6]
               [1 20 3] ; id, cw, len in cw
               [2 6 12]
               [3 2 6]
               [1 0 3] ; id, cw, len in cw
               [2 6 12]
               [3 2 6]
               [1 0 3] ; id, cw, len in cw
               [2 6 12]
               [3 2 6]
               [1 40 3] ; id, cw, len in cw
               [2 6 12]
               [3 2 6]
               [1 50 3] ; id, cw, len in cw
               [2 6 12]
               [3 42 6]
               [1 50 3] ; id, cw, len in cw
               [2 46 12]
               [3 2 60]
               [1 0 3] ; id, cw, len in cw
               [2 69 12]
               [3 2 6]
               [1 0 30] ; id, cw, len in cw
               [2 6 12]
               [3 20 6]
               [1 0 3] ; id, cw, len in cw
               [2 6 120]
               [3 2 6]])

(def p-large [[1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]
              [1 0 3] ; id, cw, len in cw
              [2 56 12]
              [3 2 6]
              [1 20 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 40 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 50 3] ; id, cw, len in cw
              [2 6 12]
              [3 42 6]
              [1 50 3] ; id, cw, len in cw
              [2 46 12]
              [3 2 60]
              [1 0 3] ; id, cw, len in cw
              [2 69 12]
              [3 2 6]
              [1 0 30] ; id, cw, len in cw
              [2 6 12]
              [3 20 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 120]
              [3 2 6]
              [1 9 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 60]
              [1 30 30] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]
              [1 10 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 40 3] ; id, cw, len in cw
              [2 16 12]
              [3 42 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]
              [1 0 3] ; id, cw, len in cw
              [2 56 12]
              [3 2 6]
              [1 20 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 40 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 50 3] ; id, cw, len in cw
              [2 6 12]
              [3 42 6]
              [1 50 3] ; id, cw, len in cw
              [2 46 12]
              [3 2 60]
              [1 0 3] ; id, cw, len in cw
              [2 69 12]
              [3 2 6]
              [1 0 30] ; id, cw, len in cw
              [2 6 12]
              [3 20 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 120]
              [3 2 6]
              [1 9 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 60]
              [1 30 30] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]
              [1 10 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 40 3] ; id, cw, len in cw
              [2 16 12]
              [3 42 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]
              [1 0 3] ; id, cw, len in cw
              [2 56 12]
              [3 2 6]
              [1 20 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 40 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 50 3] ; id, cw, len in cw
              [2 6 12]
              [3 42 6]
              [1 50 3] ; id, cw, len in cw
              [2 46 12]
              [3 2 60]
              [1 0 3] ; id, cw, len in cw
              [2 69 12]
              [3 2 6]
              [1 0 30] ; id, cw, len in cw
              [2 6 12]
              [3 20 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 120]
              [3 2 6]
              [1 9 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 60]
              [1 30 30] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]
              [1 10 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 40 3] ; id, cw, len in cw
              [2 16 12]
              [3 42 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]
              [1 0 3] ; id, cw, len in cw
              [2 56 12]
              [3 2 6]
              [1 20 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 40 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 6]
              [1 50 3] ; id, cw, len in cw
              [2 6 12]
              [3 42 6]
              [1 50 3] ; id, cw, len in cw
              [2 46 12]
              [3 2 60]
              [1 0 3] ; id, cw, len in cw
              [2 69 12]
              [3 2 6]
              [1 0 30] ; id, cw, len in cw
              [2 6 12]
              [3 20 6]
              [1 0 3] ; id, cw, len in cw
              [2 6 120]
              [3 2 6]
              [1 9 3] ; id, cw, len in cw
              [2 6 12]
              [3 2 60]
              [1 30 30] ; id, cw, len in cw
              [2 6 12]
              [3 52 6]
              [1 10 3] ; id, cw, len in cw
              [2 6 12]
              [3 22 6]
              [1 40 3] ; id, cw, len in cw
              [2 16 12]
              [3 42 6]])

#_(def pr-cur p-small)
#_(defn create-model []
    {:min-cw   (apply min (map second pr-cur))
     :max-cw   (apply max (map #(+ (last %) (second %)) pr-cur))
     :projects pr-cur})

(def d t/date)

(defn create-model []
  (model/generate-random-model 100)
  #_(-> (model/new-model "a-model")
        (model/add-resource "engineering" 20 40)
        (model/add-pipeline "pip-25" 25)
        (model/add-project "new-proj-0" nil "pip-25")
        (model/add-project "new-proj-1" nil "pip-25")
        (model/add-project "new-proj-2" nil "pip-25")
        (model/add-task "new-proj-0" (model/t (d "2020-01-07")
                                              (d "2020-06-01")
                                              "engineering"
                                              200))
        (model/add-task "new-proj-1" (model/t (d "2023-05-07")
                                              (d "2023-06-01")
                                              "engineering"
                                              200))
        (model/add-task "new-proj-2" (model/t (d "2024-05-07")
                                              (d "2024-06-18")
                                              "engineering"
                                              200))))


(comment
  (create-model)
  (.scrollTo js/window 0 0 :smooth)
  (.scrollTo js/window 100 100 :smooth)
  (.scrollTo js/window 0 0)
  (def cursor (.getElementById js/document "cursor"))
  (.scrollIntoView cursor))