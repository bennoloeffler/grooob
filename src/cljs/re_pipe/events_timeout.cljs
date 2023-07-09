(ns re-pipe.events-timeout
  (:require
    [re-frame.core :as rf]
    [reagent.core :as r]))

; could that all be done with
; :dispatch-later
; see: https://day8.github.io/re-frame/api-builtin-effects/#dispatch-later
(rf/reg-event-fx
  :set-timeout
  (fn [cfx [_ params]]
    (println "e: " (:event params))
    (println "ps: " params)
    {:fx [[:dispatch-later {:ms (:time params) :dispatch (:event params)}]]}))

(defonce timeouts (r/atom {}))

(rf/reg-fx
  :timeout
  (fn [{:keys [id event time]}]
    (when-some [existing (get @timeouts id)]
      (js/clearTimeout existing)
      (swap! timeouts dissoc id))
    (when (some? event)
      (swap! timeouts assoc id
             (js/setTimeout
               (fn []
                 (rf/dispatch event))
               time)))))

(rf/reg-event-fx
  :set-alert
  (fn [cfx [_ msg]]
    {:db (-> (:db cfx)
             (assoc :alert-message msg)
             #_(assoc :alert-message-beep true))
     :timeout {:id :alert-message
               :event [:remove-alert]
               :time 20000}
     :dispatch [:alert-blink]}))

(rf/reg-event-fx
  :alert-blink
  (fn [cfx [_ _]]
    {:db (-> (:db cfx)
             (assoc :alert-blink true))
     :timeout {:id :alert-blink
               :event [:remove-alert-blink]
               :time 100}}))

(rf/reg-event-db
  :remove-alert-blink
  (fn [db _]
    (-> db
        (assoc :alert-blink false))))

(rf/reg-event-db
  :remove-alert
  (fn [db]
    (dissoc db :alert-message)))

(rf/reg-sub
 :alert-message
 (fn [db _]
   (:alert-message db)))

(rf/reg-sub
  :alert-blink
  (fn [db _]
    (:alert-blink db)))


(comment
  #_(rf/dispatch [:alert-message "Please have a nice day!!"])
  (rf/dispatch [:set-alert "Please have a nice day!!"])
  (rf/dispatch [:alert-blink]))