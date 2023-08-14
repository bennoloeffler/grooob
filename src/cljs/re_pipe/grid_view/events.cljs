(ns re-pipe.grid-view.events
  (:require [goog.events :as events]
            [re-frame.core :as rf]
            [re-pipe.model :as model]
            [belib.browser :as bb])
  (:import [goog.events EventType]))

; TODO use as example
(rf/reg-event-fx
  :grid-view/init
  (fn [cofx [_ component-id grid cross]]
    (let [db       (:db cofx)
          comp-key (keyword component-id)]
      {:db (if (-> db :view comp-key)
             db
             (-> db
                 (assoc-in [:view comp-key :cross] (or cross {:y 0 :x 0}))
                 (assoc-in [:view comp-key :grid] (or grid 40))))})))

