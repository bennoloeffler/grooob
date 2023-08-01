(ns re-pipe.experiments
  (:require [re-frame.core :as rf]
            [belib.hiccup :as bh]
            [belib.browser :as bb]
            [reagent.core :as r]
            [goog.events :as events]
            [cljs.pprint]
            [tick.core :as t]
            [clojure.string :as str]
    ;[playback.preload]
            [re-pipe.events-timeout :as timeout])
  (:import [goog.events EventType]))



(comment
  ; mini-doc see user.clj
  (require 'playback.preload)

  ; test #> #>> #><[]
  #>>(defn make-something [a b]
       (->> (range (* a b))
            (map inc)
            (map #(* 3 %))
            (map str)))
  (make-something 3 3))

(defn now-date-time []

  (t/instant))

(comment
  (now-date-time))

;; https://fontawesome.com/search?q=user&o=r&m=free
(defn show-size []
  (let [resize (rf/subscribe [:view/resize])]
    (fn [] (let [jsw-inner-width      (.-innerWidth js/window)
                 jsd-de-client-width  (.-clientWidth (.-documentElement js/document))
                 jsd-b-client-width   (.-clientWidth (.-body js/document))
                 jsw-inner-height     (.-innerHeight js/window)
                 jsd-de-client-height (.-clientHeight (.-documentElement js/document))
                 jsd-b-client-height  (.-clientHeight (.-body js/document))]
             [:pre
              #_(bb/filter-events-map #"dr")
              #_bb/all-events-map
              #_[
                 :on-copy :on-cut :on-paste
                 :on-key-down :on-key-press :on-key-up
                 :on-focus :on-blur
                 :on-change :on-input :on-invalid :on-reset :on-submit]

              (str @resize "\n"
                   "jsw-inner-width: " jsw-inner-width "\n"
                   "jsd-de-client-width: " jsd-de-client-width "\n"
                   "jsd-b-client-width: " jsd-b-client-width "\n"
                   "jsw-inner-height: " jsw-inner-height "\n"
                   "jsd-de-client-height: " jsd-de-client-height "\n"
                   "jsd-b-client-height: " jsd-b-client-height)]))))
(defn pr-size
  "https://github.com/district0x/re-frame-window-fx"
  []
  (let [jsw-inner-width      (.-innerWidth js/window)
        jsd-de-client-width  (.-clientWidth (.-documentElement js/document))
        jsd-b-client-width   (.-clientWidth (.-body js/document))
        jsw-inner-height     (.-innerHeight js/window)
        jsd-de-client-height (.-clientHeight (.-documentElement js/document))
        jsd-b-client-height  (.-clientHeight (.-body js/document))]
    (println (str "jsw-inner-width: " jsw-inner-width "\n"
                  "jsd-de-client-width: " jsd-de-client-width "\n"
                  "jsd-b-client-width: " jsd-b-client-width "\n"
                  "jsw-inner-height: " jsw-inner-height "\n"
                  "jsd-de-client-height: " jsd-de-client-height "\n"
                  "jsd-b-client-height: " jsd-b-client-height))))




(defn change-after [time before then]
  (let [to-change (r/atom before)]
    (js/setTimeout #(reset! to-change then) time)
    to-change))

#_(defn your-component []
    (let [class-name (r/atom "is-blue")]
      (js/setTimeout #(reset! class-name "is-red") 2000) ; set the new class name after 5 seconds
      (fn []
        [:div {:class @class-name} "Hello, world!"])))

#_(defn your-component []
    [:div {:class @(change-after 3000 "is-red" "is-blue")} "Hello, world!"])

(defn your-component []
  (let [change-class (change-after 3000 "is-red" "is-blue")]
    (fn [] [:div {:class @change-class} "Hello, world!"])))

#_(defn timer-first-then [val-first val-then time]
    (println "timer-first-then")
    (let [id    (keyword (str val-first val-then time))
          _     (rf/dispatch-sync [:add-id id])
          _     (rf/reg-sub
                  id
                  (fn [db _]
                    (get-in db [:ids id])))
          first (rf/subscribe [id])
          _     (rf/dispatch [:set-timeout {:id    id
                                            :event [:remove-id id]
                                            :time  time}])]
      (fn [] (if @first val-first val-then))))

(defn login-with-google-button []
  (let [bounce-on-off (change-after 3000 "fa-bounce" nil)]
    (fn []
      [:a.button.m-1
       (merge #_bb/all-events-map
         {} #_(bb/filter-events-map #"drag")
         {:href "/login-with-google"})
       ;:on-click #(rf/dispatch [:user/login-with-google])}
       [:span.icon.is-large>i (bh/cs "fas" "fa-1x" "fa-brands" @bounce-on-off "fa-google")]
       [:span "login with google"]])))

(defn create-user-button []
  [:a.button.m-1
   {:on-click #(rf/dispatch [:user/add-random-user])}
   [:span.icon.is-large>i.fas.fa-1x.fa-user-plus]
   [:span "create random user cofx"]])


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


(defn create-time-button []
  [:a.button.is-primary.is-large.is-rounded.m-1
   {:on-click #(rf/dispatch [:set-current-time])}
   [:span.icon.is-large>i.fas.fa-1x.fa-clock]
   [:span "time? cofx"]])

(defn create-blink-button []
  [:a.button.m-1.is-small
   {:on-click #(rf/dispatch [:alert-blink])}
   [:span.icon>i.fas.fa-1x.fa-bolt-lightning]
   [:span "blink with event-timer"]])

;; A view that displays the current time
(defn current-time-view []
  (let [t (rf/subscribe [:current-time])]
    (fn []
      [:span "Current time: " @t])))

(defn authorized-user-button []
  [:a.button.mr-1.m-1
   {#_:href #_"#/authorized" :on-click #(rf/dispatch [:user/authorized])}
   [:span.icon.is-large>i.fas.fa-1x.fa-user-tag]
   [:span "call with user logged in"]])

(defn logout-user-button []
  [:a.button.is-light.m-1
   {:href "#/logout" :on-click #(rf/dispatch [:user/logout])}
   [:span.icon.is-large>i.fas.fa-1x.fa-right-from-bracket]
   [:span "logout"]])

(defn login-button []
  [:a.button.is-primary.is-outlined.m-1
   {:href "#/"}
   [:span.icon.is-large>i.fas.fa-1x.fa-sign-in-alt]
   [:span "login"]])

(defn user []
  (let [user (rf/subscribe [:user])]
    (fn []
      [:<>
       [:div "current user: " [:strong (:identity @user)]]])))

(defn experiments []
  [:div
   [:h1.title "Experiments go here"]
   [show-size]
   [user]
   [:br]
   [create-blink-button]
   [:br]
   [:br]

   [create-time-button]
   [current-time-view]
   [:br]
   [:br]
   [:br]
   [create-user-button]
   [users-component]
   [:br]
   [login-with-google-button] [login-button] [authorized-user-button] [logout-user-button]
   [:br]
   [your-component]])




