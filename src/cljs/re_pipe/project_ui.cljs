(ns re-pipe.project-ui
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

; https://tech.toryanderson.com/2020/05/23/adding-custom-transit-handlers-to-re-frame-http-fx-ajax-requests/
; https://github.com/cognitect/transit-cljs/wiki/Getting-Started
; https://gist.github.com/jdf-id-au/2e91fb63ce396b722c1d6770154f1815
; https://github.com/henryw374/time-literals

#_(time-literals.read-write/print-time-literals-cljs!)
#_(cljs.reader/register-tag-parser! time-literals.read-write/tags)
#_(doall (map #(cljs.reader/register-tag-parser! (first %) (second %)) time-literals.read-write/tags))
#_(cljs.reader/register-tag-parser! 'time/date (time-literals.read-write/tags 'time/date))

#_(clojure.edn/read-string {:readers time-literals.read-write/tags} "#time/date \"2011-01-01\"")
;(clojure.edn/read-string "#time/date \"2011-01-01\"")
;(println (t/date "2011-01-01"))

#_(comment
    (time-literals.read-write/print-time-literals-cljs!)
    (time-literals.read-write/print-time-literals-clj!)
    (map #(cljs.reader/register-tag-parser! (first %) (second %)) time-literals.read-write/tags)

    (time-literals.read-write/print-time-literals-cljs!)
    (cljs.reader/register-tag-parser! 'time/date (time-literals.read-write/tags 'time/date))
    (def d #time/date "2039-01-01")
    (time-literals.read-write/tags 'time/period))



#_(defn scrollCursorVisible []
    (let [cursor (.getElementById js/document "cursor")]
      (.scrollIntoView cursor (clj->js {:behavior "smooth", :block "nearest", :inline "nearest"}))))

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

#_(defn cross-pos []
    (let [cross (rf/subscribe [:view/cross])
          model (rf/subscribe [:model/model])]
      (fn []

        (let [now (bc/week-year-from-abs-week (+ (:min-cw @model) (:cw @cross)))]
          [:<>
           #_[:a.button.is-small.m-1
              {:on-click #(rf/dispatch [:user/add-random-user])}
              [:span.icon>i.fas.fa-lg.fa-chart-gantt]
              [:span "open project view"]]
           #_[:div (str now ", " @cross ", min-max-cw: " (:min-cw @model) ", " (:max-cw @model) ", now: " (bc/get-abs-current-week))]
           [:div (str ((vec (:projects @model)) (:project @cross)))]]))))


#_(defn weeks-from-abs-weeks [start-week num-weeks]
    (vec (map (fn [e] (bc/week-year-from-abs-week (+ start-week e 1)))
              (range num-weeks))))

#_(defn weeks-indicators [all-year-weeks]
    (map #(str (first %) "-" (second %)) all-year-weeks))

#_(comment
    (weeks-from-abs-weeks 0 10)
    (weeks-from-abs-weeks 100 10)
    (weeks-indicators (weeks-from-abs-weeks 100 10)))

#_(defn week [indicator cw g y]
    [:text {:x                 (+ (* cw g) (/ g 2))
            :y                 (+ y g)
            :fill              "black"
            ;:font-weight       "bold"
            :dominant-baseline "middle"
            :font-size         (* 0.8 g)
            :writing-mode      "tb"}

     (str indicator)])

#_(defn weeks []
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


#_(defn cursor []
    (let [cross (rf/subscribe [:view/cross])
          model (rf/subscribe [:model/model])
          grid  (rf/subscribe [:view/grid])]
      (fn []
        (let [g           @grid
              c           @cross
              x           (:cw c)
              y           (:project c)
              ;_           (println "y: " y)
              ;_           (println (:projects @model))
              p-indicator (:name ((vec (:projects @model)) y))
              ;_           (println p-indicator)
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
           [:text {:x                 (+ (* x g) (/ g 2))
                   :y                 (+ (* g (+ y 2)))
                   :fill              "grey"
                   :font-family       "Nunito" #_"verdana" #_"arial" #_"titillium web"
                   :font-weight       "bold"
                   :dominant-baseline "middle"
                   :font-size         (* 0.8 g)
                   :writing-mode      "tb"
                   :fill-opacity      0.5}

            (first (weeks-indicators [cursor-week]))]
           [:text {:x                 (* g (+ 2 x))
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
#_(defn cross []
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


#_(defn square []
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

#_(defn project-names []
    (let [grid           (rf/subscribe [:view/grid])
          browser-scroll (rf/subscribe [:view/browser-scroll])]
      (fn [row start-cw len-cw name project-id]
        (let [gx (* start-cw @grid)
              gy (* row @grid)]
          (vec (cons :<> (conj (vec (map (fn [cw] [square (+ gx (* cw @grid)) gy])
                                         (range len-cw)))
                               [:text {:x                 (+ (get-svg-x-offset) (* 2 @grid))
                                       :y                 (+ (/ @grid 2) gy)
                                       :fill              "black"
                                       ;:font-weight       "bold"
                                       :dominant-baseline "middle"
                                       :font-size         (* 0.8 @grid)
                                       :dummy             @browser-scroll} ; just to update the :x by (get-svg-x-offset)
                                (str name)])))))))

#_[:text {:x    10
          :y    gy
          :fill "blue"}
   (str 123)]


#_(defn projects []
    (let [model (rf/subscribe [:model/model])]
      (fn []
        (let [projects      (:projects @model)
              start-cw      (:min-cw @model)
              projects-html (vec (cons :<> (map (fn [{:keys [idx start-x len-x name id]}]
                                                  [project-names idx (- start-x start-cw) len-x name id])
                                                projects)))]
          ;(pprint start-cw)
          ;(pprint projects-html)
          projects-html))))

#_(declare scroll-fn)


#_(defn t [svg-offset-x svg-offset-y & text-vals]
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


#_(defn dragging [from to]
    (pprint [from :to to]))
#_(def dragging-debounced (bb/debounced-now dragging 200))

#_(def start-dragging (atom nil))

#_(defn get-x-y-vec [event]
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

;; TODO set the listeners to the right div
#_(events/listen js/window
                 EventType.MOUSEDOWN
                 (fn [event] (reset! start-dragging {:x (.-offsetX event)
                                                     :y (.-offsetY event)})))
#_(defn mouse-down-fn [event] (reset! start-dragging (get-x-y-vec event)))
#_(events/listen js/window
                 EventType.MOUSEUP
                 #(reset! start-dragging nil))
#_(defn mouse-up-fn [event] (reset! start-dragging nil))

#_(events/listen js/window
                 EventType.MOUSEMOVE
                 (fn [event] (when @start-dragging
                               (dragging-debounced @start-dragging {:x (.-offsetX event)
                                                                    :y (.-offsetY event)}))))
#_(defn mouse-move-fn [event] (when @start-dragging
                                (dragging-debounced @start-dragging (get-x-y-vec event))))

#_(defn click-fn [event]
    #_(bb/prjs event)
    (let [;e    (js->clj event)
          ;em   (bb/js-obj->clj-map event)
          {:keys [x y]} (get-x-y-vec event)]
      (rf/dispatch [:view/cross-abs-move x y])))


#_(def ignore-prevent-default {187 true
                               189 true})
#_(def shift-down (atom false))
#_(defn capture-key
    "Given a `keycode`, execute function `f` "
    ; see https://github.com/reagent-project/historian/blob/master/src/cljs/historian/keys.cljs
    [keycode-map]
    (let [
          press-fn (fn [key-press]
                     (let [key-code (.. key-press -keyCode)]
                       (when (= 16 key-code)
                         (reset! shift-down true))
                       (when-let [f (get keycode-map key-code)]
                         (when-not (ignore-prevent-default key-code)
                           (.preventDefault key-press))
                         (f))))]

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

#_(defn key-down-fn [key-press]
    (println "key-down X" key-press))

#_(defn key-up-fn [key-press]
    (println "key-up Y" key-press)
    #_(when (= 16 (.. key-press -keyCode))
        (reset! shift-down false)))




#_(defn projects+cross []
    (let [browser-size   (rf/subscribe [:view/browser-size])
          browser-scroll (rf/subscribe [:view/browser-scroll])
          grid           (rf/subscribe [:view/grid])
          m              (rf/subscribe [:model/model])
          cross-data     (rf/subscribe [:view/cross])
          registered     (atom nil)]

      (fn []
        (let [

              svg-offset-x   100
              div            (.getElementById js/document "divContainer")
              svg            (.getElementById js/document "svgElement")
              body           (.-body js/document)
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


           [:svg {:id            "svgElement"
                  :style         {:background-color "lightgray"}
                  ;+:viewBox    (str "0 0 " 1000 " " (/ 1000 xy-ratio))
                  :width         x-px #_"100vw"
                  :height        y-px #_"100vh"
                  :font-family   "Nunito"
                  :on-click      click-fn
                  :on-mouse-down mouse-down-fn
                  :on-mouse-move mouse-move-fn
                  :on-mouse-up   mouse-up-fn}
            ;:on-key-down   key-down-fn
            ;:on-key-up     key-up-fn}
            ;:onLoad     #(println "load svg")}
            #_(register-all-listeners (.getElementById js/document "svgElement"))
            [cross]
            [projects]
            [weeks]
            [cursor]
            [:circle {:cx           0 :cy 0 :r 25
                      :stroke       "white"
                      :stroke-width 1
                      :fill-opacity 0.7
                      :fill         utils/background-color}]
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

            [:circle {:cx           x-px :cy y-px
                      :r            25
                      :stroke       "white"
                      :stroke-width 1
                      :fill-opacity 0.7
                      :fill         utils/background-color}]]]))))


#_(def project-view-keydown-rules
    {:event-keys [

                  ;; Move PROJECT with shift <- -> AD

                  [[:view/project-move 1]
                   ;; will be triggered if
                   [{:keyCode keycodes/RIGHT :shiftKey true}]
                   [{:keyCode keycodes/D :shiftKey true}]] ;  AD

                  [[:view/project-move -1]
                   ;; will be triggered if
                   [{:keyCode keycodes/LEFT :shiftKey true}]
                   [{:keyCode keycodes/A :shiftKey true}]] ;  AD

                  ;; Move CURSOR / CROSS with <- ->

                  [;; this event
                   [:view/cross-move {:cw 0 :project -1}]
                   ;; will be triggered if
                   [{:keyCode keycodes/UP}]
                   ;; or
                   [{:keyCode keycodes/W}]] ;  WASD
                  ;; is pressed

                  [[:view/cross-move {:cw 0 :project 1}]
                   ;; will be triggered if
                   [{:keyCode keycodes/DOWN}]
                   [{:keyCode keycodes/S}]] ;  WASD

                  [[:view/cross-move {:cw 1 :project 0}]
                   ;; will be triggered if
                   [{:keyCode keycodes/RIGHT}]
                   [{:keyCode keycodes/D}]] ;  WASD

                  [[:view/cross-move {:cw -1 :project 0}]
                   ;; will be triggered if
                   [{:keyCode keycodes/LEFT}]
                   [{:keyCode keycodes/A}]] ;  WASD

                  ;; ZOOM with  + / -  or with  z / shift-z

                  [[:view/zoom 0.9]
                   ;; will be triggered if
                   [{:keyCode keycodes/DASH}]
                   [{:keyCode keycodes/Z :shiftKey true}]]

                  [[:view/zoom 1.1]
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
;; is pressed

#_(def debounce-scroll-fn
    (bb/debounced-now #_goog.functions.debounce
      (fn dispatch-browser-scroll [event]
        ;(cljs.pprint/pprint (b/js-obj->clj-map event))
        (rf/dispatch [:view/browser-scroll]))
      100))

#_(defn scroll-fn [event]
    ;(println "scroll" event)
    (debounce-scroll-fn event)
    ;(cljs.pprint/pprint (b/js-obj->clj-map event))
    #_(goog.functions.debounce #(rf/dispatch [:view/browser-scroll]) 300)
    #_(rf/dispatch [:view/browser-scroll]))

#_(events/listen js/window
                 EventType.SCROLL
                 scroll-fn)


#_(defn render-projects-overview-form []
    (fn [id]
      [:<>
       ;[:br]
       ;[cross-pos]
       [projects+cross]]))

#_(defn projects-overview-form []
    (let [scroll-listener (atom nil)]
      (reagent/create-class
        {:display-name           "projects-view=logged-in-form"
         :reagent-render         render-projects-overview-form
         :component-did-mount    (fn []
                                   (println "Projects view mounted. Install event listeners.")
                                   (rf/dispatch [::rp/set-keydown-rules
                                                 project-view-keydown-rules])
                                   (reset! scroll-listener (events/listen (.getElementById js/document "divContainer")
                                                                          EventType.SCROLL
                                                                          scroll-fn))

                                   #_(let [kdl (events/listen js/window
                                                              EventType.KEYDOWN
                                                              (fn [key-press]
                                                                (println "key down")))
                                           kul (events/listen js/window
                                                              EventType.KEYUP
                                                              (fn [key-press]
                                                                (println "key up")))]
                                       (reset! key-down-listener kdl)
                                       (reset! key-up-listener kul)))

         :component-will-unmount (fn []
                                   (println "Projects view will unmount. Uninstall event listeners.")
                                   (rf/dispatch [::rp/set-keydown-rules
                                                 {}])
                                   (events/unlistenByKey @scroll-listener)
                                   #_(events/unlistenByKey @key-up-listener))})))

#_(defn project-form []
    (let [cross   (rf/subscribe [:view/cross])
          model   (rf/subscribe [:model/model])
          project (rf/subscribe [:model/current-project])]
      (fn []
        [:<>
         [:div "project: " (str (last (get (vec (:projects @model)) (:project @cross))))]
         ;[:pre (with-out-str (pprint @model))]
         [:pre (with-out-str (pprint @project))]
         [psv/project-single-view "project-form"]])))

#_(comment
    (def l (events/listen js/window
                          EventType.KEYUP #_(-> KeyHandler .-EventType .-KEY)
                          (fn [key-press]
                            (println "key pressed"))))
    ;(def listener-key ((bb/js-obj->clj-map l) "listener"))
    (events/unlistenByKey l)
    #_[:pre (with-out-str (pprint @re-frame.db/app-db))]
    #_[:pre (str "logged in: " @(rf/subscribe [:user]))]
    nil)

#_(rf/reg-event-fx
    :view/cross-up
    (fn [_ _]
      {:fx [[:view/cross-move {:cw 0 :project (- 1)}]
            [:view/cross-visible]]}))

#_(rf/reg-event-db
    :view/cross-visible
    (fn [db [_ data]]
      (scrollCursorVisible)))

#_(rf/reg-event-fx
    :view/cross-move

    (fn [cofx [_ data]]
      (let [db          (:db cofx)
            new-cross   (merge-with +
                                    (get-in db [:view :cross])
                                    data)
            g           (get-in db [:view :grid])
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
        {:db       (assoc-in db [:view :cross]
                             valid-cross)
         :dispatch [:grid-view/cross-visible]})))

#_(rf/reg-event-db
    :view/cross-abs-move
    (fn [db [_ x y]]
      (let [g      (get-in db [:view :grid])
            cw     (quot x g)
            pr     (quot y g)
            model  (get-in db [:model])
            size-y (count (:g/projects model))]
        ;(println "x/y: "x"/"y ", cw/pr: "cw"/"pr)
        (if (< pr size-y)
          (assoc-in db [:view :cross]
                    {:cw cw :project pr})
          db))))


#_(rf/reg-event-fx
    :view/project-move
    (fn [cofx [_ cw]]
      (let [db      (:db cofx)
            cross   (get-in db [:view :cross])
            pr-keys (vec (keys (get-in db [:model :g/projects])))
            pr-id   (pr-keys (:project cross))]
        {:db       (-> db
                       (update :model model/move-project pr-id (* 7 cw))
                       (update-in [:view :cross :cw] + cw))
         :dispatch [:grid-view/cross-visible]})))


#_(rf/reg-event-db
    :view/zoom
    (fn [db [_ zoom-factor]]
      ;(println "zoom" zoom-factor)
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

#_(rf/reg-sub
    :view/cross
    (fn [db _]
      (-> db :view :cross)))

; TODO make it a derived one
#_(rf/reg-sub
    :view/size
    (fn [db _]
      (-> db :view :size)))



#_(rf/reg-sub
    :view/browser-scroll
    (fn [db _]
      (-> db :view :browser-scroll)))

; todo remove
#_(rf/reg-sub
    :view/resize
    (fn [db _]
      (-> db :view :resize)))

#_(rf/reg-sub
    :model/projects
    (fn [db _]
      (get-in db [:model :projects])))

#_(rf/reg-sub
    :model/model
    (fn [db _]
      (model/view-model (:model db))))

#_(rf/reg-sub
    :model/current-project
    (fn [db _]
      (let [current-project-key (-> (:model db)
                                    :g/projects
                                    keys
                                    vec
                                    (get (-> db :view :cross :project)))
            current-project     (-> (:model db)
                                    :g/projects
                                    (get current-project-key))]
        (model/current-project current-project))))


#_(rf/reg-sub
    :view/grid
    (fn [db _]
      (-> db :view :grid)))

#_(def ignore-prevent-default {187 true
                               189 true})

#_(def shift-down (atom false))
#_(defn capture-key
    "Given a `keycode`, execute function `f` "
    ; see https://github.com/reagent-project/historian/blob/master/src/cljs/historian/keys.cljs
    [keycode-map]
    (let [;key-handler (KeyHandler. js/document)
          press-fn (fn [key-press]
                     (let [key-code (.. key-press -keyCode)]
                       (when (= 16 key-code)
                         (reset! shift-down true))
                       (when-let [f (get keycode-map key-code)]
                         (when-not (ignore-prevent-default key-code)
                           (.preventDefault key-press))
                         (f))))]

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


#_(comment
    (type (KeyHandler. js/document))
    (def l (events/listen (KeyHandler. js/document) #_js/window
                          EventType.KEYUP #_(-> KeyHandler .-EventType .-KEY)
                          (fn [key-press]
                            (println "key pressed")
                            (println (bb/js-obj->clj-map key-press)))))
    (def listener-key ((bb/js-obj->clj-map l) "listener"))
    (events/unlistenByKey l))



#_(def p-small [[1 0 3] ; id, cw, len in cw
                [2 6 12]
                [3 22 6]
                [1 0 3] ; id, cw, len in cw
                [2 6 12]
                [3 52 6]])

#_(def p-medium [[3 2 6]
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

#_(def p-large [[1 0 3] ; id, cw, len in cw
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

#_(def d t/date)

#_(defn create-model []
    (model/generate-random-model 30)
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


#_(comment
    #_(create-model)
    (.scrollTo js/window 0 0 :smooth)
    (.scrollTo js/window 100 100 :smooth)
    (.scrollTo js/window 0 0)
    (def cursor (.getElementById js/document "cursor"))
    (.scrollIntoView cursor))

#_(def time-deserialization-handlers
    (assoc-in time/time-deserialization-handlers
              [:handlers "LocalDate"] (transit/read-handler #(t/date %))))

#_(def time-serialization-handlers
    (assoc-in time/time-serialization-handlers
              [:handlers LocalDate] (transit/write-handler
                                      (constantly "LocalDate")
                                      #(str %))))

;(belib.browser/js-obj->clj-map time-serialization-handlers)

;(def date (t/date "2023-04-03"))
;(println "TYPE")
;(println (= (type date) java.time.LocalDate))
;(println java.time.LocalDate)

#_(comment
    (defn transit-out [data]
      (let [w (transit/writer :json time-serialization-handlers)]
        (transit/write w data)))

    (defn transit-in [data]
      (let [r (transit/reader :json time-deserialization-handlers)]
        (transit/read r data)))

    (println "t/date OUT: " (transit-out (t/date "2023-04-03")))
    (transit-out {:x "y"})
    (def date-transit (transit-out (t/date)))
    (def restored-date (transit-in date-transit)))


#_(rf/reg-event-fx
    :model/save
    (fn [cofx _]
      (println "saving model:")
      ;(pprint (:model (:db cofx))) ;TODO remove load data???
      {:http-xhrio {:method          :post
                    :params          (:model (:db cofx))
                    :uri             "/api/send-recv-date"
                    :format          (ajax/transit-request-format #_ajax/json-request-format
                                       time-serialization-handlers)
                    :response-format (ajax/transit-response-format #_(ajax/raw-response-format {:keywords? true})
                                       time-deserialization-handlers)
                    :on-success      [:authorized-success]
                    :on-failure      [:authorized-failure]}}))

#_(comment
    (type (#time/date "2039-01-01")))