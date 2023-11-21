(ns grooob.core
  (:require
    [belib.malli :as bm]
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [markdown.core :refer [md->html]]
    [grooob.ajax :as ajax]
    [grooob.experiments :as ex]
    [grooob.events]
    [grooob.utils :as utils]
    [grooob.model.model-malli :as model]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string]
    [belib.hiccup :as bh]
    [grooob.events-timeout]
    #_[grooob.project-ui :as ui]
    [grooob.comps.ui :as cui]
    [grooob.project-single-view.ui :as psv]
    [grooob.projects-overview.ui :as pov]
    [grooob.project-details.ui :as pdv]
    [grooob.raw-data-view.ui :as rdv]
    [re-pressed.core :as rp]
    [grooob.debug.playback]
    [grooob.debug.portfolio :as ui-test]
    [grooob.ui-login.core :as ui-login]
    #_[grooob.debug.scenes]
    [grooob.debug.on-off-ui-tests :as on-off-ui-tests])
  (:import goog.History
           [goog.events EventType KeyHandler]))

; TODO: needed or is requiring enough?
;(grooob.playback/load)

;(println (model/now-date-time))

; REAGENT basis for re-frame
; https://github.com/reagent-project/reagent/blob/master/doc/CreatingReagentComponents.md

; RE-FRAME docs
; https://github.com/Day8/re-frame/blob/2ccd95c397927c877fd184b038e4c754221a502d/docs/Effects.md

; RE-FRAME from Eric Normand
; https://ericnormand.me/guide/re-frame-building-blocks

; REACTIVE from db to ui
; postgres notifications: reactive... https://yogthos.net/posts/2016-11-05-LuminusPostgresNotifications.html

; RE-FRAME TYPICAL Problem
; local state & laggy input: https://github.com/day8/re-frame/blob/master/docs/FAQs/laggy-input.md

; RE-FRAME COMPONENTS - but not : re-com
; Demo: https://re-com.day8.com.au/
; Doc: https://github.com/day8/re-com


(defn logout-user-button [classes]
  [:a.button
   (merge {:href "#/logout" :on-click #(rf/dispatch [:user/logout])}
          (bh/cs classes))
   [:span.icon>i.fas.fa-1x.fa-right-from-bracket]
   [:span "logout"]])

(defn logout-link []
  [:a.navbar-item
   {:href "#/logout" :on-click #(rf/dispatch [:user/logout])}
   [:span.icon>i.fas.fa-1x.fa-right-from-bracket]
   [:span "logout"]])

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href  uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []

  (r/with-let [expanded? (r/atom false)]
              (let [user-data (rf/subscribe [:user])]
                [:nav.navbar.has-background-light>div.container
                 [:div.navbar-brand
                  [:a.navbar-item {:href "/" :style {:font-weight :bold}} [:img {:src "img/glasses-solid.png"}]]
                  [:span.navbar-burger.burger
                   {:data-target :nav-menu
                    :on-click    #(swap! expanded? not)
                    :class       (when @expanded? :is-active)}
                   [:span] [:span] [:span]]]
                 [:div#nav-menu.navbar-menu
                  {:class (when @expanded? :is-active)}
                  [:div.navbar-start

                   [nav-link "#/models-list" (if @user-data "Your Data" "Demo") :models-list]
                   [nav-link "#/about" "About" :about]
                   ;[nav-link "#/ex" "Experiments" :experiments]
                   ;[nav-link "#/projects-portfolio" "Projects" :projects-portfolio]
                   ;[nav-link "#/project" "Project" :project]
                   ;[nav-link "#/project-details" "Details" :project-details]
                   (when @user-data
                     [:<>
                      [nav-link "#/user-profile" (:identity @user-data) :user-profile]
                      [logout-link]])
                   (when-not @user-data
                     [nav-link "#/" "Login" :home])]]


                 [:div.navbar-end
                  [:div.navbar-item.mr-3]]])))

;[tb/test-button]


(defn about-page []
  [:section.section>div.container
   [:h1.title "grooob"]
   [:h4.subtitle "capacity planning " [:br] "without distracting details"]
   [:div "who did it:" [:br] [:span.has-text-primary " Benno Löffler"]]
   [:br]
   [:div "how to contact:" [:br]
    #_[:span.has-text-primary "benno.loeffler AT gmx.de"]
    [:a {:href "mailto:benno.loeffler@gmx.de"} "benno.loeffler AT gmx.de"]]
   [:a {:href "https://www.linkedin.com/in/benno-l%C3%B6ffler-8b10929a/"} "Benno Löffler - on LinkedIn"]
   [:br]
   [:br]
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
   [ui-login/register-form]])

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

(defn project-details-page []
  (let [user-data (rf/subscribe [:user])]
    (fn []
      [:<>
       #_(if @user-data
           [:div "logged in: " [:b (:identity @user-data)]]
           [:div "logged in: NO"])
       [pdv/project-details-view "project-details-form"]])))

(defn models-list-page
  []
  (let [user-data (rf/subscribe [:user])]
    (fn []
      [:<>
       [cui/overview-proj-details-menu]
       #_(if @user-data
           [:div "logged in: " [:b (:identity @user-data)]]
           [:div "logged in: NO"])
       [:div "models list"]])))


(defn projects-portfolio-page []
  (let [user-data (rf/subscribe [:user])]
    (fn []
      [:<>
       #_(if @user-data
           [:div "logged in: " [:b (:identity @user-data)]]
           [:div "logged in: NO"])

       [pov/projects-overview-form "projects-overview-form"]])))

(defn raw-data-page []
  (let [user-data (rf/subscribe [:user])]
    (fn []
      [:<>
       #_(if @user-data
           [:div "logged in: " [:b (:identity @user-data)]]
           [:div "logged in: NO"])

       [rdv/raw-data-form]])))


(defn user-profile-page []
  (let [user-data       (rf/subscribe [:user])
        created-account (rf/subscribe [:sub/data-path [:created-account]])
        is-email        (fn [val] (bm/hum-err bm/email-schema val))
        data            (atom {})]
    (fn []
      [:<>
       #_[ui/projects-overview-form]
       #_[:div
          [:h1.title "grooob.com"
           [:h4.subtitle "capacity planning without distracting details"]]]
       (if @user-data
         [:div.container>div.content
          [:div.columns.is-mobile.is-gapless
           [:div.column.is-11
            #_[:input.input.is-fullwidth]
            [:div.field [cui/input-field data :user :email "invite other users by email" "fa-user-plus" is-email]]]
           [:div.column.is-1
            [:div.field [:button.button.is-fullwidth
                         [:span.icon>i.fas.fa-plus]]]]]
          #_[:div (str "user logged in: " (:identity @user-data))]
          #_[:div.columns
             [:div.column.is-12
              [:div.field
               [logout-user-button "is-fullwidth is-primary is-outlined"]]]]

          ;[:div "add credit card & upgrade to team-license"]
          [:div.columns
           [:div.column.is-12
            [:div "add an additional email-adress as alternative login"]]]
          [:div.columns.is-mobile.is-gapless
           [:div.column.is-11
            #_[:input.input.is-fullwidth]
            [:div.field [cui/input-field data :user :email "additional login, email" "fa-envelope" is-email]]]
           [:div.column.is-1
            [:div.field [:button.button.is-fullwidth
                         [:span.icon>i.fas.fa-plus]]]]]
          [:br]
          [:br]
          [:br]
          [:br]
          [:div.columns
           [:div.column.is-12
            [:button.button.is-fullwidth.is-danger.is-outlined
             [:b (str "delete account of " (:identity @user-data))]
             [:span.icon
              [:i.fas.fa-warning]]]]]]


         [:div "not logged in"])])))

(defn home-page []
  (let [user-data       (rf/subscribe [:user])
        created-account (rf/subscribe [:sub/data-path [:created-account]])]
    (fn []
      [:<>
       #_[ui/projects-overview-form]
       #_[:div
          [:h1.title "grooob.com"
           [:h4.subtitle "capacity planning without distracting details"]]]
       (if @user-data
         [user-profile-page]
         #_[pov/projects-overview-form "projects-overview-form"] #_[ui/projects-overview-form]
         [ui-login/login-form @created-account])])))


(defn home-page-from-google []
  [:<>
   [:div "welcome back from google login..."]
   [:br] [:br]
   [models-list-page]])

(defn home-page-from-facebook []
  [:<>
   [:div "welcome back from facebook login..."]
   [:br] [:br]
   [models-list-page]])




(defn page []
  (let [page        (rf/subscribe [:common/page])
        alert-msg   (rf/subscribe [:alert-message])
        alert-blink (rf/subscribe [:alert-blink])]
    (fn []
      ;(println "ui-test: " @on-off-ui-tests/ui-test)
      ;(println "page: " @page)
      (if @on-off-ui-tests/ui-test
        (do (ui-test/start)
            [:div])
        (if @page
          [:div
           [navbar]
           [:div {:style {:padding-top  "4px"
                          :padding-left "30px"
                          :font-weight  "bold"
                          :color        "white"
                          :height       (if @alert-msg "40px" "5px")
                          :background   (if @alert-msg utils/primary-color "white")
                          :transition   "font-size 1s, height 500ms"
                          ;:visibility (if @alert-msg "visible" "hidden")
                          :font-size    (if @alert-msg "20px" "5px")}} @alert-msg]

           [:section.section {:style {:background (if @alert-blink utils/primary-color "white" #_utils/primary-color)}}
            ;[:div.container>div.content
            [@page]]]
          [:div
           [navbar]
           [:div.container-404
            [:section.section [:div.text-404 "Oooops..."] [:div.text-404 "Page not found..."]]]])))))



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
     ["/project-details" {:name        :project-details
                          :view        #'project-details-page
                          :controllers [#_{:start (fn [_] (rf/dispatch [:view/init]))}]}]
     ["/raw-data" {:name        :raw-data
                   :view        #'raw-data-page
                   :controllers [#_{:start (fn [_] (rf/dispatch [:view/init]))}]}]

     ["/google-login" {:name        :google-login
                       :view        #'home-page-from-google
                       :controllers [{:start (fn [req]
                                               (rf/dispatch [:login-google req])
                                               #_(rf/dispatch [:view/init]))}]}]
     ["/facebook-login" {:name        :facebook-login
                         :view        #'home-page-from-facebook
                         :controllers [{:start (fn [req]
                                                 (rf/dispatch [:login-facebook req])
                                                 #_(rf/dispatch [:view/init]))}]}]
     ["/user-profile" {:name :user-profile
                       :view #'user-profile-page}]
     ["/models-list" {:name :models-list
                      :view #'models-list-page}]]))






(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))


(defn do-strange []
  (println "something strange" (grooob.events/from-events)))

(comment ; this can be done by repl...
  (rf/dispatch [:common/navigate! :projects-portfolio])
  (println "switch to console to view this"))


(comment ; this can be done by repl...
  (println "X switch to console to view this")
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
                       model/test-model-with-start-end #_(model/generate-random-model 20)
                       #_(model/generate-simplest-model)))}))


(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components)
  (println "init! model and view")
  (rf/dispatch-sync [:model/init])
  ;(rf/dispatch-sync [:view/init])
  (rf/dispatch-sync
    [::rp/add-keyboard-event-listener "keydown"])

  (rf/dispatch-sync [:common/navigate! :home])

  #_(ui/register-key-handler))


(comment
  (shadow/repl :app))


