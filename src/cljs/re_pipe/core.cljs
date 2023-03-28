(ns re-pipe.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [re-pipe.ajax :as ajax]
    [re-pipe.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History))

; https://ericnormand.me/guide/re-frame-building-blocks
; postgres notifications: reactive... https://yogthos.net/posts/2016-11-05-LuminusPostgresNotifications.html
; local state & laggy input: https://github.com/day8/re-frame/blob/master/docs/FAQs/laggy-input.md
; components: re-com


(defn input-field
  "Assoc data-atom key value-of-input at every key stroke."
  [data-atom key type placeholder]
  [:input.input.is-primary
   {:type      (name type) :placeholder placeholder
    :on-change #(let [val (-> % .-target .-value)]
                  (swap! data-atom assoc key val))}])

(defn login-button []
  [:a.button.is-primary.is-outlined {:href "#/login"} #_{:on-click #(reset! logged-in true)} [:span.icon.is-large>i.fas.fa-1x.fa-sign-in-alt] [:span "login"]])

(defn register-button []
  [:a.button..is-outlined.is-darkgray.mr-1
   {:href "#/register" :on-click #(rf/dispatch [:messages/load])}
   [:span.icon.is-large>i.fas.fa-1x.fa-pen-nib]
   [:span "create free account"]])

(defn authorized-user-button []
  [:a.button.is-warning.mr-1
   {#_:href #_"#/authorized" :on-click #(rf/dispatch [:user/authorized])}
   [:span.icon.is-large>i.fas.fa-1x.fa-pen-nib]
   [:span "call with user logged in"]])

(defn logout-user-button []
  [:a.button.is-warning.mr-1
   {:href "#/logout" :on-click #(rf/dispatch [:user/logout])}
   [:span.icon.is-large>i.fas.fa-1x.fa-pen-nib]
   [:span "logout"]])


(defn login-form []
  (fn []
    (let [data (atom {:user "" :pw ""})]
      (fn []
        [:div.container
         [:div.columns
          [:div.column.is-5
           [:div.columns.is-flex.is-flex-direction-column
            [:div.column
             [:label {:for "email"} "Email"]
             [input-field data :user :text "Email adress"]]
            [:div.column
             [:label {:for "Name"} "Password"]
             [input-field data :pw :password "Password"]
             [:a.is-size-7.has-text-primary {:href "forget.html"} "forget password?"]]
            [:div.column
             [:button.button.is-primary.is-fullwidth
              {:type     "submit"
               :on-click #(rf/dispatch [:user/login (:user @data) (:pw @data)])}
              "Login"]]
            [:div.has-text-centered
             [:p.is-size-7 "Don't have a free account? " [:a.has-text-primary {:href "#"} [:b " Sign up"]]]]]]]]))))


(defn register-form []
  (fn []
    (let [data (atom {:name "" :email "" :pw "" :pw-repeat ""})]
      [:div.container ; columns.is-flex.is-flex-direction-column.box
       [:div.columns
        [:div.column.is-5
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
          [input-field data :pw-repeat :password "repeat Password"]
          [:input {:type "checkbox"}] " I agree to the" [:a.has-text-primary {:href "#"} " terms and conditions"]]
         [:div.column
          [:button.button.is-primary.is-fullwidth
           {:type "submit"
            :on-click #(rf/dispatch [:user/register @data])}
           "Create an account"]]
         [:div.has-text-centered
          [:p "Already have an account?" [:a.has-text-primary {:href "#"} [:b " Login"]]]]]]])))

(defn create-user-button []
  [:a.button.is-warning.mr-1
   {:on-click #(rf/dispatch [:user/add-random-user])}
   [:span.icon.is-large>i.fas.fa-1x.fa-pen-nib]
   [:span "create random user"]])

(defn create-time-button []
  [:a.button.is-warning.mr-1
   {:on-click #(rf/dispatch [:set-current-time])}
   [:span.icon.is-large>i.fas.fa-1x.fa-pen-nib]
   [:span "time?"]])


(defn user-list-component []
  [:a.button.is-warning.mr-1
   {:href "#/register" :on-click #(rf/dispatch [:messages/load])}
   [:span.icon.is-large>i.fas.fa-1x.fa-pen-nib]
   [:span "create free account"]])

(defn users-component []
  (let [u (rf/subscribe [:current-random-user])]
    (fn []
      (let [user @u]
        ;(println user)
        [:div
         [:div "User:"
          [:div (:user/id user)]
          [:div (:user/name user)]
          [:div (:user/email user)]
          [:div (:user/password user)]
          [:div (:user/status user)]]]))))




(defn nav-link [uri title page]
  [:a.navbar-item
   {:href  uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.has-background-light>div.container
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
                 [nav-link "#/about" "About" :about]]]
               [:div.navbar-end
                [:div.navbar-item.mr-3]]]))

                 ;[tb/test-button]




(defn about-page []
  [:section.section>div.container>div.content
   [:div
    [:h1.title "grooob"]
    [:h4.subtitle "capacity without distracting details"]
    #_[:iframe.giphy-embed {:src "https://giphy.com/embed/pNsBooBjruKnm" :width "720" :height "960" :frameBorder "0" :allowFullScreen "true"}]
    [:pre "sources" [:a {:href "https://giphy.com/gifs/campfire-pNsBooBjruKnm"} "via GIPHY"]]]])

(defn logout-page [data]
  [:section.section>div.container>div.content
   [:div
    [:h1.title "grooob"]
    [:h4.subtitle "logged out"]
    [:p data]]])


;; A view that displays the current time
(defn current-time-view []
  (let [t (rf/subscribe [:current-time])]
    (fn []
      [:div "Current time: " @t])))

(defn home-page []
  [:section.section>div.container>div.content
   [:div
    [:h1.title "grooob.com"]
    [:h4.subtitle "capacity planning without distracting details"]
    [:div.buttons
     [register-button]
     [login-button]]
    [create-user-button]
    [users-component]
    [create-time-button]
    [current-time-view]
    [:br] [:br] [:br]
    [login-form]
    [:br] [:br] [:br]
    [register-form]
    [:br] [:br] [:br]
    [authorized-user-button][logout-user-button]]


   #_(when-let [docs @(rf/subscribe [:docs])]
       [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  ;(println match)
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]
     ["/logout" {:name :logout
                 :view #'logout-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

(defn do-strange []
  (println "something strange" (re-pipe.events/from-events)))

(comment ; this can be done by repl...
  (println "switch to console to view this")
  (js/alert "hallo from repl...")
  (do-strange)
  (reitit/route-names router)
  (reitit/match-by-name router :about)
  (.getElementById js/document "app")
  (rdom/render "oha... please reload :-)" (.getElementById js/document "app")))

; TODO: view reframe db... change it...



;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))








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

#_(defn capture-key
    "Given a `keycode`, execute function `f` "
    ; see https://github.com/reagent-project/historian/blob/master/src/cljs/historian/keys.cljs
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


#_(defn reagent-content-fn []
      ;; sets up the event listener
      (capture-key {keycodes/ENTER #(println "Luna Lovegood")
                    keycodes/K #(swap! state update :shift dec)
                    keycodes/L #(swap! state update :shift inc)}))

#_(defn nav-link [uri title page]
    [:a.navbar-item
     {:href   uri
      :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
     title])

#_(defn navbar []
    (r/with-let [expanded? (r/atom false)]
                [:nav.navbar.is-info>div.container
                 [:div.navbar-brand
                  [:a.navbar-item {:href "/" :style {:font-weight :bold}} "grooob.com"]
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

#_(defn about-page []
    [:section.section>div.container>div.content
     [:h3 "about-text"]
     [:div "text"]])

#_(defn rect [x y w h f s sw]
    [:rect {:x x
            :y y
            :rx 2
            :ry 2
            :width w
            :height h
            :fill f
            :stroke s
            :stroke-width sw}])

#_(defn row [x y])

#_(defn grid-sub-components []
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

#_(defn grid-basic []
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



#_(defn home-page []
    [:section.section>div.container>div.content
     [:h3  "experiments..."]
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
             :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
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
    #_(reagent-content-fn))
