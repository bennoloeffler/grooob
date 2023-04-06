(ns re-pipe.experiments
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [re-pipe.events-timeout :as timeout]))

;; https://fontawesome.com/search?q=user&o=r&m=free

(defn cs
  "Join classes together.
  This functions may be called with expressions.
  Instead of [:i.fas.fa-1x.fa-user-plus
  you may call [:i.fas (cs (if (< @size 2) \"fa-3x\" \"fa-31x\"))"
  [& names]
  {:class (str/join " " (filter identity names))})

(defn timer-first-then [val-first val-then time]
  (rf/dispatch [:set-timeout {:id (keyword (str val-first val-then)) ; create id in set-timeout
                              :event [:remove-alert]
                              :time 20000}])
  val-first)

(defn login-with-google-button []
  [:a.button.m-1
   {:href "/login-with-google"}
    ;:on-click #(rf/dispatch [:user/login-with-google])}
   [:span.icon.is-large>i (cs "fas" "fa-1x" "fa-brands" #_(timer-first-then "fa-bounce" nil 1000) "fa-google")]
   [:span "login with google"]])

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
   [login-with-google-button][login-button][authorized-user-button][logout-user-button]])




