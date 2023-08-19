(ns re-pipe.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [markdown.core :refer [md->html]]
    [re-pipe.ajax :as ajax]
    [re-pipe.experiments :as ex]
    [re-pipe.events]
    [re-pipe.utils :as utils]
    [re-pipe.model :as model]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string]
    [belib.hiccup :as bh]
    [re-pipe.events-timeout]
    #_[re-pipe.project-ui :as ui]
    [re-pipe.project-single-view.ui :as psv]
    [re-pipe.projects-overview.ui :as pov]
    [re-pressed.core :as rp]
    [re-pipe.playback])
  (:import goog.History
           [goog.events EventType KeyHandler]))

; TODO: needed or is requiring enough?
;(re-pipe.playback/load)

;(println (model/now-date-time))

; REFRAME docs
; https://github.com/Day8/re-frame/blob/2ccd95c397927c877fd184b038e4c754221a502d/docs/Effects.md

; REFRAME from Eric Normand
; https://ericnormand.me/guide/re-frame-building-blocks

; REACTIVE from db to ui
; postgres notifications: reactive... https://yogthos.net/posts/2016-11-05-LuminusPostgresNotifications.html

; TYPICAL Problem
; local state & laggy input: https://github.com/day8/re-frame/blob/master/docs/FAQs/laggy-input.md

; COMPONENTS - but not : re-com
; Demo: https://re-com.day8.com.au/
; Doc: https://github.com/day8/re-com


(defn change-once-after
  "Returns a reagent/atom with value before.
  Changes it once after time to
  value then.
  Example:
  (defn login-with-google-button-1 []
    (let [bounce-off (change-once-after 3000 \"fa-bounce\" nil)]
      (fn []
        [:span.icon.is-large>i
          (bh/cs @bounce-off \"fas fa-lg fa-brands fa-google\")])))"
  [time before then]
  (let [to-change (r/atom before)]
    (js/setTimeout #(reset! to-change then) time)
    to-change))

(defn change-continuously
  "returns a reagent/atom and changes it every
  intervall milliseconds to the next value
  in the collection coll."
  [intervall coll]
  (let [idx       (atom 0)
        to-change (r/atom (get coll @idx))]
    (letfn [(change-it []
              (swap! idx inc)
              (when (< @idx (count coll))
                (reset! to-change (get coll @idx))
                (js/setTimeout change-it intervall)))]
      (js/setTimeout change-it intervall)
      to-change)))

(defn fa-smaller []
  (change-continuously 35 ["fa-10x" "fa-9x" "fa-8x" "fa-7x" "fa-6x" "fa-5x" "fa-4x" "fa-3x" "fa-2x" "fa-1x" "fa-sm" "fa-xs" "fa-2xs" "fa-2xs" "fa-xs" "fa-sm" "fa-1x" "fa-lg"]))

(defn fa-bounce-off []
  (change-once-after 3000 "fa-bounce" nil))

(defn input-field
  "Assoc data-atom key value-of-input at every key stroke."
  [data-atom key type placeholder]
  [:input.input.is-primary
   {:type      (name type) :placeholder placeholder
    :on-change #(let [val (-> % .-target .-value)]
                  (swap! data-atom assoc key val))}])


(defn register-button []
  (let [smaller (fa-smaller)]
    (fn []
      [:a.button.is-light.is-outlined.mr-1.is-fullwidth
       {:href "#/register"}
       [:span.icon.is-large>i (bh/cs @smaller "fas fa-pen-nib")]
       [:span "create free account"]])))


(defn login-with-google-button []
  (let [bounce-off (fa-bounce-off)]
    (fn []
      [:a.button.is-light.is-outlined.mr-1.is-fullwidth
       {:href "/login-with-google"}
       [:span.icon.is-large>i (bh/cs @bounce-off "fas fa-lg fa-brands fa-google")]
       [:span "login with google account"]])))

; https://lambdaisland.com/episodes/passwordless-authentication-ring-oauth2

(defn login-form []
  (fn []
    (let [data (atom {:user "" :pw ""})]
      (fn []
        [:div.container
         [:div.columns
          [:div.column.is-5

           [:div.columns.is-flex.is-flex-direction-column
            [:br]
            ;[:br]
            [:div.column [register-button]]
            [:div.divider "or login with your google account"]
            [:div.column [login-with-google-button]]
            ;[:br]
            [:div.divider "or login with email and password"]
            [:div.column
             [:label {:for "email"} "Email"]
             [input-field data :user :text "Email adress"]]
            [:div.column
             [:label {:for "Name"} "Password"]
             [input-field data :pw :password "Password"]
             [:a.is-size-7.has-text-primary {:href "#/forget-password"} "forget password?"]]
            [:div.column
             [:button.button.is-light.is-outlined.is-fullwidth {:on-click #(rf/dispatch [:user/login (:user @data) (:pw @data)])}
              [:span.icon.is-large>i.fas.fa-1x.fa-sign-in-alt] [:span "login"]]
             #_[:button.button.is-primary.is-fullwidth
                {:type     "submit"
                 :on-click #(rf/dispatch [:user/login (:user @data) (:pw @data)])}
                "Login"]]
            [:div.has-text-centered
             [:p.is-size-7 "Don't have a free account? " [:a.has-text-primary {:href "#/register"} [:b " Sign up"]]]]]]]]))))


(defn register-form []
  (fn []
    (let [data (atom {:name "" :email "" :pw "" :pw-repeat ""})]
      [:div.container ; columns.is-flex.is-flex-direction-column.box
       [:div.columns
        [:div.column.is-5
         [:div.columns.is-flex.is-flex-direction-column
          [:div.column
           [:label {:for "name"} "Name"]
           [input-field data :name :text "Enter Name - optional"]]
          [:div.column
           [:label {:for "email"} "Email"]
           [input-field data :email :text "Email address"]]
          [:div.column
           [:label {:for "Name"} "Password"]
           [input-field data :pw :password "Password"]]
          [:div.column
           [:label {:for "Name"} "repeat Password"]
           [input-field data :pw-repeat :password "repeat Password"]]

          [:div.column
           [:label.checkbox-container
            [:input.input {:type      "checkbox"
                           :on-change #(let [val (-> % .-target .-checked)]
                                         (println val)
                                         (swap! data assoc :terms val))}]
            [:span.checkbox-checkmark {:style {:margin-top "3px"}}] [:div " I agree to the" [:a.has-text-primary {:href "#"} " terms and conditions"]]]]





          ;[:label.checkbox [:input {:type "checkbox"}] " Remember me"]]
          ;[:input {:type "checkbox"}] " I agree to the" [:a.has-text-primary {:href "#"} " terms and conditions"]]
          [:div.column
           [:button.button.is-light.is-outlined.is-fullwidth
            {:type     "submit"
             :on-click #(rf/dispatch [:user/register @data])}
            [:span [:b "Create an account"]]]]
          [:div.has-text-centered
           [:p "Already have an account?" [:a.has-text-primary {:href "#"} " Login"]]]]]]])))



(defn nav-link [uri title page]
  [:a.navbar-item
   {:href  uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.has-background-light>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} [:img {:src "img/healthcare-skull-icon-23.png"}]]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click    #(swap! expanded? not)
                  :class       (when @expanded? :is-active)}
                 [:span] [:span] [:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]
                 [nav-link "#/about" "About" :about]
                 [nav-link "#/ex" "Experiments" :experiments]
                 [nav-link "#/projects-portfolio" "Projects" :projects-portfolio]
                 [nav-link "#/project" "Project" :project]]]
               [:div.navbar-end
                [:div.navbar-item.mr-3]]]))

;[tb/test-button]


(defn about-page []
  [:div
   [:h1.title "grooob"]
   [:h4.subtitle.is-white "capacity planning " [:br] "without distracting details"]
   [:div "who did it:" [:br] [:span.is-white " Benno Löffler"]] [:br]
   [:div "how to contact:" [:br] [:span.is-white " benno.loeffler AT gmx.de"]] [:br]
   [:div "where to find sources:" [:br] [:a {:href "https://github.com/bennoloeffler/grooob"} " here on github"]]])

(defn logout-page [data]
  [:section.section>div.container>div.content
   [:div
    [:h1.title "grooob"]
    [:h4.subtitle "logged out"]
    [:p data]]])

(defn register-page []
  [:div
   [:h1.title "grooob.com"]
   [:h4.subtitle "register a free account"]
   ;[:br]
   ;[:br]
   [register-form]])

(defn user []
  (let [user (rf/subscribe [:user])]
    (fn []
      [:span "user: " [:strong (:identity @user)]])))

(defn project-single-page []
  (let [user-data (rf/subscribe [:user])]
    (fn []
      [:<>
       #_(if @user-data
           [:div "logged in: " [:b (:identity @user-data)]]
           [:div "logged in: NO"])
       [psv/project-single-view "project-single-form"]])))

(defn projects-portfolio-page []
  (let [user-data (rf/subscribe [:user])]
    (fn []
      [:<>
       #_(if @user-data
           [:div "logged in: " [:b (:identity @user-data)]]
           [:div "logged in: NO"])

       [pov/projects-overview-form "projects-overview-form"]])))


(defn home-page []
  (let [user-data (rf/subscribe [:user])]
    (fn []
      [:<>
       #_[ui/projects-overview-form]
       #_[:div
          [:h1.title "grooob.com"
           [:h4.subtitle "capacity planning without distracting details"]]]
       (if @user-data
         [pov/projects-overview-form "projects-overview-form"] #_[ui/projects-overview-form]
         [login-form])])))


(defn home-page-from-google []
  [:<>
   [:div "welcome back from google login..."]
   [:br] [:br]
   [home-page]])


(defn page []
  (let [page        (rf/subscribe [:common/page])
        alert-msg   (rf/subscribe [:alert-message])
        alert-blink (rf/subscribe [:alert-blink])]
    (fn []
      (if @page
        [:div
         [navbar]
         [:div {:style {:padding-top  "4px"
                        :padding-left "30px"
                        :font-weight  "bold"
                        :color        "white"
                        :height       (if @alert-msg "40px" "5px")
                        :background   (if @alert-msg "darkred" "white")
                        :transition   "font-size 1s, height 500ms"
                        ;:visibility (if @alert-msg "visible" "hidden")
                        :font-size    (if @alert-msg "20px" "5px")}} @alert-msg]

         [:section.section {:style {:background (if @alert-blink "darkred" utils/background-color)}} [:div.container>div.content]
          [@page]]]
        [:div
         [navbar]
         [:div.container-404
          [:section.section [:div.text-404 "Oooops..."] [:div.text-404 "Page not found..."]]]]))))



(defn navigate! [match _]
  ;(println match)
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [#_{:start (fn [_] (rf/dispatch [:view/init]))}]}]
     ["/about" {:name :about
                :view #'about-page}]
     ["/logout" {:name :logout
                 :view #'logout-page}]
     ["/register" {:name :register
                   :view #'register-page}]
     ["/ex" {:name :experiments
             :view #'ex/experiments}]
     ["/projects-portfolio" {:name        :projects-portfolio
                             :view        #'projects-portfolio-page
                             :controllers [#_{:start (fn [_] (rf/dispatch [:view/init]))}]}]
     ["/project" {:name        :project
                  :view        #'project-single-page
                  :controllers [#_{:start (fn [_] (rf/dispatch [:view/init]))}]}]

     ["/google-login" {:name        :google-login
                       :view        #'home-page-from-google
                       :controllers [{:start (fn [req]
                                               (rf/dispatch [:login-google req])
                                               #_(rf/dispatch [:view/init]))}]}]]))


(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))


(defn do-strange []
  (println "something strange" (re-pipe.events/from-events)))

(comment ; this can be done by repl...
  (rf/dispatch [:common/navigate! :projects-portfolio])
  (println "switch to console to view this"))


(comment ; this can be done by repl...
  (println "switch to console to view this")
  (js/alert "hallo from repl...")
  (do-strange)
  (reitit/route-names router)
  (reitit/match-by-name router :about)
  (.getElementById js/document "app")
  (rdom/render "oha... reload please :-)" (.getElementById js/document "app")))



;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(rf/reg-event-fx
  :model/init
  (fn [cofx _]
    {:db (-> (:db cofx)
             (assoc-in [:model]
                       (model/generate-random-model 100)))}))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components)
  (println "init! model and view")
  (rf/dispatch-sync [:model/init])
  ;(rf/dispatch-sync [:view/init])
  (rf/dispatch-sync
    [::rp/add-keyboard-event-listener "keydown"])

  #_(ui/register-key-handler))


(comment
  (shadow/repl :app))






#_(ns re-pipe.core
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
#_(def state (r/atom {:shift 0}))
#_(defn handle-event [event] ;an event object is passed to all events
    (println (str "BELs event: " event)))


#_(defn nav-link [uri title page]
    [:a.navbar-item
     {:href  uri
      :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
     title])

#_(defn navbar []
    (r/with-let [expanded? (r/atom false)]
                [:nav.navbar.is-info>div.container
                 [:div.navbar-brand
                  [:a.navbar-item {:href "/" :style {:font-weight :bold}} "grooob.com"]
                  [:span.navbar-burger.burger
                   {:data-target :nav-menu
                    :on-click    #(swap! expanded? not)
                    :class       (when @expanded? :is-active)}
                   [:span] [:span] [:span]]]
                 [:div#nav-menu.navbar-menu
                  {:class (when @expanded? :is-active)}
                  [:div.navbar-start
                   [nav-link "#/" "Home" :home]
                   [nav-link "#/about" "About" :about]]]]))

#_(defn about-page []
    [:section.section>div.container>div.content
     [:h3 "about-text"]
     [:div "text"]])

#_(defn rect [x y w h f s sw]
    [:rect {:x            x
            :y            y
            :rx           2
            :ry           2
            :width        w
            :height       h
            :fill         f
            :stroke       s
            :stroke-width sw}])

#_(defn row [x y])

#_(defn grid-sub-components []
    (let [grid-width  10
          square-size 8
          localShift  (:shift @state)]
      ;shift-state (* w (@state :shift))
      ;shift-state-str (str shift-state)]
      [:section.section>div.container>div.content
       [:div ""] ;shift-state-str]
       [:svg {:view-box "0 0 1000 1000"
              :width    1800
              :height   1800}
        (for [x (range 100) y (range 100) :let [shift (if (zero? y) (* localShift grid-width) 0)]]
          ^{:key (+ y (* x 1000))} [rect (+ shift (* grid-width x)) (* grid-width y) square-size square-size "lightgray" nil 0])

        #_[:rect {:x      20 :y 50 :width 50
                  :height 50
                  :fill   "blue"}]]]))
;[:line {:stroke "red" :stroke-width 1 :x1 0 :y1 25 :x2 25 :y2 100}]]]))

#_(defn grid-basic []
    [:section.section>div.container>div.content
     [:div "-->"]
     [:svg {:view-box "0 0 100 100"
            :width    100
            :height   100}
      [:rect {:width  50
              :height 50
              :fill   "green"}
       [:rect {:x      20 :y 50 :width 50
               :height 50
               :fill   "blue"}]
       [:line {:stroke "red" :stroke-width 1 :x1 0 :y1 25 :x2 25 :y2 100}]]]])



#_(defn home-page []
    [:section.section>div.container>div.content
     [:h3 "experiments..."]
     ;[:p "here comes the grid:"]
     #_[grid-sub-components]
     #_[grid-basic]
     #_[:br]
     #_(when-let [docs @(rf/subscribe [:docs])]
         [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

#_(defn page []
    (if-let [page @(rf/subscribe [:common/page])]
      [:div
       #_[navbar]
       [page]]))

#_(defn navigate! [match _]
    (rf/dispatch [:common/navigate match]))

#_(def router
    (reitit/router
      [["/" {:name        :home
             :view        #'home-page
             :controllers [{:start (fn [_] (rf/dispatch [:view/init]))}]}]
       ["/about" {:name :about
                  :view #'about-page}]]))

#_(defn start-router! []
    (rfe/start!
      router
      navigate!
      {}))

;; -------------------------
;; Initialize app
#_(defn ^:dev/after-load mount-components []
    (rf/clear-subscription-cache!)
    (rdom/render [#'page] (.getElementById js/document "app")))

#_(defn init! []
    (start-router!)
    (ajax/load-interceptors!)
    (mount-components)
    #_(register-key-handler))
