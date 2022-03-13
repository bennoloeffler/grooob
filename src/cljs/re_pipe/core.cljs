(ns re-pipe.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as gev]
    [goog.events.KeyCodes :as keycodes]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [re-pipe.ajax :as ajax]
    [re-pipe.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History)
  (:import [goog.events EventType KeyHandler]))
(def state (r/atom {:shift 0}))
(defn handle-event [event] ;an event object is passed to all events
  (println (str "BELs event: " event)))

(defn capture-key
  "Given a `keycode`, execute function `f` "
  [keycode-map]
  (let [key-handler (KeyHandler. js/document)
        press-fn (fn [key-press]
                   (println key-press)
                   (if-let [f (get keycode-map (.. key-press -keyCode))]
                     (f)
                     (println (.. key-press -keyCode))))]
    (println "register listeners")
    (gev/listen key-handler
                EventType.KEYDOWN #_(-> KeyHandler .-EventType .-KEY)
                press-fn)))


(defn reagent-content-fn []
    ;; sets up the event listener
    (capture-key {keycodes/ENTER #(println "Luna Lovegood")
                  keycodes/K #(swap! state update :shift dec)
                  keycodes/L #(swap! state update :shift inc)}))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "re-pipe"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]
                 [nav-link "#/about" "About" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:h3 "about-text"]
   [:div "text"]])

(defn rect [x y w h f s sw]
  [:rect {:x x
          :y y
          :rx 2
          :ry 2
          :width w
          :height h
          :fill f
          :stroke s
          :stroke-width sw}])

(defn row [x y])

(defn grid-sub-components []
 (let [grid-width 10
       square-size 8
       localShift (:shift @state)]
       ;shift-state (* w (@state :shift))
       ;shift-state-str (str shift-state)]
  [:section.section>div.container>div.content
     [:div ""] ;shift-state-str]
     [:svg {:view-box "0 0 1000 1000"
            :width 1800
            :height 1800}
      (for [x (range 100) y (range 100) :let [shift (if (zero? y) (* localShift grid-width) 0)]]
        ^{:key (+ y (* x 1000))} [rect (+ shift (* grid-width x)) (* grid-width y) square-size square-size "lightgray" nil 0])

      #_[:rect {:x 20 :y 50 :width 50
                :height 50
                :fill "blue"}]]]))
      ;[:line {:stroke "red" :stroke-width 1 :x1 0 :y1 25 :x2 25 :y2 100}]]]))

(defn grid-basic []
  [:section.section>div.container>div.content
    [:div "-->"]
    [:svg {:view-box "0 0 100 100"
           :width 100
           :height 100}
          [:rect {:width 50
                  :height 50
                  :fill "green"}]
          [:rect {:x 20 :y 50 :width 50
                  :height 50
                  :fill "blue"}]
          [:line {:stroke "red" :stroke-width 1 :x1 0 :y1 25 :x2 25 :y2 100}]]])



(defn home-page []
  [:section.section>div.container>div.content
   [:h3  "experiments..."]
   ;[:p "here comes the grid:"]
   [grid-sub-components]
   [grid-basic]
   [:br]
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components)
  (reagent-content-fn))
