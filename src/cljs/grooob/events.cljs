(ns grooob.events
  (:require
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [day8.re-frame.http-fx]
    [reitit.frontend.easy :as rfe]
    [reitit.frontend.controllers :as rfc]))

;;dispatchers

(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                                 (rfc/apply-controllers (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

;; GENERAL SUBSCRIPTION
;; example (rf/subsribe :sub/data-path [:view :grid :x])
;; (rf/reg-sub
;;  :current-time
;;  (fn [db _]
;;    (:current-time db)))
;; would be: (rf/subsribe :sub/data-path [:current-time])
(rf/reg-sub
  :sub/data-path
  (fn [db [_ path]]
    (get-in db path)))

#_(rf/reg-event-db
    :set/data-path
    (fn [db [_ path data]]
      (assoc-in db path data)))

#_(rf/dispatch [:set/data-path [:model :something] "New Value"])
#_(rf/reg-event-db
    :set-docs
    (fn [db [_ docs]]
      (assoc db :docs "docs")))

#_(rf/reg-event-fx
    :fetch-docs
    (fn [_ _]
      {:http-xhrio {:method          :get
                    :uri             "/docs"
                    :response-format (ajax/raw-response-format)
                    :on-success      [:set-docs]}}))
(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

;; Define a co-effect to get the current time
(rf/reg-cofx
  :current-time
  (fn [cofx _]
    ;(println "cofx system-time: " (.now js/Date))
    (assoc cofx :current-time (.now js/Date))))

;; Define an event to get the current time and store it in the app-db
(rf/reg-event-fx
  :set-current-time
  [(rf/inject-cofx :current-time)]
  (fn [cofx _]
    ;(println "event :set-current-time")
    (let [time (:current-time cofx)
          db   (:db cofx)]
      ;(println "time: " time)
      {:db (assoc db :current-time time)})))
;:dispatch [:alert-blink]})))

;; Define a subscription that reads the current time from the app-db
(rf/reg-sub
  :current-time
  (fn [db _]
    (:current-time db)))


(rf/reg-cofx
  :user/random
  (fn [cofx _]
    (assoc cofx
      :user/random {:user/id       (str (rand-int 1000000) "-abc")
                    :user/name     "Armin Benno Cargo"
                    :user/password "w4nk2lk3jh45mbn"
                    :user/email    "abc@example.com"
                    :user/status   :user.status/active})))

(rf/reg-event-fx
  :user/add-random-user
  [(rf/inject-cofx :user/random)]
  (fn [cofx _]
    (let [u  (cofx :user/random)
          db (cofx :db)]
      ;(println "event: u=" u)
      {:db (assoc db :current-random-user u)})))

(rf/reg-event-db
  :login-success
  (fn [db [_ data]]
    (println "login success: " data)
    (-> db
        (assoc :user data)
        (dissoc :user-tmp))))

(rf/reg-event-fx
  :login-failure
  (fn [db [_ data]]
    (println "login error: " data)
    ;(assoc db :user "login failed")
    {
     :fx [[:dispatch [:set-alert (str (-> data :response :message))]]
          #_[:http-xhrio {:method :GET :url "http://somewhere.com/"}]
          #_(when (> 2 3) [:full-screen true])]}))

(rf/reg-event-fx
  :user/login
  (fn [cofx [_ user pw]]
    (println "event :user/login, data= " user ", " pw)
    {
     :db         (-> (:db cofx)
                     (assoc :user-tmp user))
     ;(assoc :tmp-pw pw))

     :http-xhrio {:method          :post
                  :params          {:user user :pw pw}
                  :uri             "/api/login"
                  :format          (ajax/json-request-format)
                  :response-format (ajax/transit-response-format {:keywords? true})
                  ;:response-format (ajax/raw-response-format {:keywords? true})
                  :on-success      [:login-success]
                  :on-failure      [:login-failure]}}))


(rf/reg-event-db
  :register-success
  (fn [db [_ data]]
    (println "register success: " data)
    db))

(rf/reg-event-fx
  :register-failure
  (fn [db [_ data]]
    (println "register-error: " data)
    ; :common/set-error
    ;(assoc db : "register failed")))
    {
     :fx [[:dispatch [:set-alert (str (-> data :response :message))]]
          #_[:http-xhrio {:method :GET :url "http://somewhere.com/"}]
          #_(when (> 2 3) [:full-screen true])]}))

(rf/reg-event-fx
  :user/register
  (fn [cofx [_ data]]
    (println "event :user/register, data= " data)
    {
     ;:db         (-> (:db cofx)
     ;                (assoc :tmp-user user)
     ;                (assoc :tmp-pw pw)

     :http-xhrio {:method          :post
                  :params          data
                  :uri             "/api/register"
                  :format          (ajax/json-request-format)
                  :response-format (ajax/transit-response-format {:keywords? true})
                  :on-success      [:register-success]
                  :on-failure      [:register-failure]}}))

(rf/reg-event-fx
  :login-google
  (fn [cofx [_ data]]
    {:http-xhrio {:method          :post
                  :params          data
                  :uri             "/api/login-google"
                  :format          (ajax/json-request-format)
                  :response-format (ajax/transit-response-format {:keywords? true})
                  :on-success      [:login-success]
                  :on-failure      [:login-failure]}}))

(rf/reg-event-db
  :login-google-success
  (fn [db [_ data]]
    (println "login-google success: " data)
    (dissoc db :user)))

(rf/reg-event-db
  :logout-success
  (fn [db [_ data]]
    (println "logout success: " data)
    (dissoc db :user)))

(rf/reg-event-db
  :logout-failure
  (fn [db [_ data]]
    (println "logout-error: " data)
    ; :common/set-error
    ;(assoc db : "register failed")))
    (dissoc db :user)))

(rf/reg-event-fx
  :user/logout
  (fn [cofx [_ data]]
    (println "event :user/logout, data= " data)
    {
     ;:db         (-> (:db cofx)
     ;                (assoc :tmp-user user)
     ;                (assoc :tmp-pw pw)

     :http-xhrio     {:method          :post
                      :params          data
                      :uri             "/api/logout"
                      :format          (ajax/json-request-format)
                      :response-format (ajax/raw-response-format {:keywords? true})
                      :on-success      [:logout-success]
                      :on-failure      [:logout-failure]}
     :dispatch-later {:ms 3000 :dispatch [:common/navigate! :home]}}))
#_[:dispatch-later {:ms 200 :dispatch [:event-id1 "param"]}]

(rf/reg-event-db
  :authorized-success
  (fn [db [_ data]]
    (println "authorized success: " data)
    db))

(rf/reg-event-fx
  :authorized-failure
  (fn [_ [_ data]]
    (println "authorized-error: " data)
    ; :common/set-error
    ;(assoc db : "register failed")))
    {
     :fx [[:dispatch [:set-alert "ERROR: you are not logged in!"]]
          #_[:http-xhrio {:method :GET :url "http://somewhere.com/"}]
          #_(when (> 2 3) [:full-screen true])]}))

(rf/reg-event-fx
  :user/authorized
  (fn [cofx [_ data]]
    (println "event :user/authorized, data= " data)
    {
     ;:db         (-> (:db cofx)
     ;                (assoc :tmp-user user)
     ;                (assoc :tmp-pw pw)

     :http-xhrio {:method          :post
                  :params          data
                  :uri             "/api/authorized"
                  :format          (ajax/json-request-format)
                  :response-format (ajax/raw-response-format {:keywords? true})
                  :on-success      [:authorized-success]
                  :on-failure      [:authorized-failure]}}))


;;subscriptions

;; https://day8.github.io/re-frame/subscriptions/


(rf/reg-sub
  :current-random-user
  (fn [db _]
    (-> db :current-random-user)))

(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :user
  (fn [db _]
    (:user db)))




(defn from-events []
  {:from :events})

(rf/reg-event-db
  :add-id
  (fn [db [_ id]]
    (assoc-in db [:ids id] true)))

(rf/reg-event-db
  :remove-id
  (fn [db [_ id]]
    (println "REMOVE")
    (update-in db [:ids] dissoc id)))


