(ns grooob.ui-login.core
  (:require [grooob.comps.ui :as cui]
            [belib.hiccup :as bh]
            [belib.malli :as bm]
            [re-frame.core :as rf]
            [reagent.core :as r]))


(defn register-button []
  (let [smaller (cui/fa-smaller)]
    (fn []
      [:a.button.is-outlined.mr-1.is-fullwidth.is-primary
       {:href "#/register"}
       [:span.icon.is-large>i (bh/cs @smaller "fas fa-pen-nib")]
       [:span [:b "create free account"]]])))


(defn login-with-google-button []
  (let [bounce-off (cui/fa-bounce-off)]
    (fn []
      [:a.button.is-outlined.mr-1.is-fullwidth
       {:href "/login-with-google"}
       [:span.icon.is-large>i (bh/cs @bounce-off "fas fa-lg fa-brands fa-google")]
       [:span "login with google account"]])))

(defn login-with-facebook-button []
  (let [bounce-off (cui/fa-bounce-off)]
    (fn []
      [:a.button.is-outlined.mr-1.is-fullwidth
       {:href "/login-with-facebook"}
       [:span.icon.is-large>i (bh/cs @bounce-off "fas fa-lg fa-brands fa-facebook")]
       [:span "login with facebook account"]])))


; https://lambdaisland.com/episodes/passwordless-authentication-ring-oauth2

(defn login-form [created-new-account-message]
  (fn []
    (let [data (atom {:user "" :pw ""})]
      (fn [created-new-account-message]
        [:div.container #_{:style {:background utils/background-color}}
         [:div.columns
          [:div.column.is-5

           [:div.columns.is-flex.is-flex-direction-column
            (if-not created-new-account-message
              [:<>
               [:br]
               ;[:br]
               [:div.column [register-button]]
               ;[:div.divider "or login with your google account"]
               [:div.divider "or login"]
               [:div.column [login-with-google-button]]
               [:div.column [login-with-facebook-button]]
               ;[:div.divider "or"]
               [:br]]
              [:div.column created-new-account-message])

            [:div.column
             [:label {:for "email"} "Email"]
             [cui/input-field data :user "email" "Email adress" "fa-envelope"]]
            [:div.column
             [:label {:for "Name"} "Password"]
             [cui/input-password-field data :pw "type password here..." "fa-lock" #_#(identity nil)]
             [:a.has-text-primary {:href "#/forget-password"} "Forgot password?"]]
            [:div.column
             [:button.button.is-outlined.is-fullwidth {:on-click #(rf/dispatch [:user/login (:user @data) (:pw @data)])}
              [:span.icon.is-large>i.fas.fa-1x.fa-sign-in-alt] [:span "login"]]
             #_[:button.button.is-primary.is-fullwidth
                {:type     "submit"
                 :on-click #(rf/dispatch [:user/login (:user @data) (:pw @data)])}
                "Login"]]
            [:div.has-text-centered
             [:p "Don't have an account? " [:a.has-text-primary {:href "#/register"} [:b " Sign up for free account"]]]]]]]]))))

(defn register-form []
  (let [data         (r/atom {:name "" :email "" :pw "" :pw-repeat "" :terms false})
        pw-match-err (fn [local-data] (when-not (= (:pw @data) local-data) ["passwords do not match"]))
        email-err    (fn [val] (bm/hum-err bm/email-schema val))]

    (fn []
      [:div.container ; columns.is-flex.is-flex-direction-column.box
       [:div.columns
        [:div.column.is-5
         [:div.columns.is-flex.is-flex-direction-column
          #_[:div.column
             ;[:label {:for "name"} "Short Name"]
             [cui/input-field data :name :text "enter short name - optional" "fa-user"]]
          [:div.column
           ;[:label {:for "email"} "Email"]
           [cui/input-field data :email :text "enter email address" "fa-envelope" email-err false true]]
          [:div.column
           ;[:label {:for "Name"} "Password"]
           ;[data-atom key type placeholder icon-left user-validate-fn validating-from-start write-through]
           [cui/input-password-field data :pw "enter password" nil nil true]]
          [:div.column
           ;[:label {:for "Name"} "repeat Password"]
           ; [data-atom key placeholder icon-left validator-fn write-through]
           [cui/input-password-field
            data :pw-repeat
            "repeat password"
            nil
            (fn match-passwords [local-data] (pw-match-err local-data))
            true]]
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
           [:button.button.is-outlined.is-fullwidth.is-primary
            {:type     "submit"
             :disabled (or (not (:terms @data))
                           (cui/errors-in-password (:pw @data))
                           (pw-match-err (:pw-repeat @data))
                           (email-err (:email @data)))
             :on-click #(rf/dispatch [:user/register @data])}
            [:span [:b "Create my account"]]
            [:span.icon.is-small
             [:i.fas.fa-user]]]]
          [:div.has-text-centered
           [:p "Already have an account? Then " [:a.has-text-primary {:href "#"} [:b " Login"]]]]]]]])))


