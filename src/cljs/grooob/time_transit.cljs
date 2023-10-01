(ns grooob.time-transit
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]
            [reagent.dom :as rd]
            [tick.core :as t]
            [java.time :refer [LocalDate]]
            [goog.events.KeyCodes :as keycodes]
            [goog.events :as events]
            [goog.object :as gobj]
            [goog.functions]
            [clojure.pprint :refer [pprint]]
            [belib.browser :as bb]
            [belib.spec :as bs]
    ;[grooob.model :as model]
    ;[grooob.model-spec :as ms]
    ;[grooob.utils :as utils]
    ;[belib.cal-week-year :as bc]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]))
;[grooob.project-single-view.ui :as psv]))

; https://tech.toryanderson.com/2020/05/23/adding-custom-transit-handlers-to-re-frame-http-fx-ajax-requests/
; https://github.com/cognitect/transit-cljs/wiki/Getting-Started
; https://gist.github.com/jdf-id-au/2e91fb63ce396b722c1d6770154f1815
; https://github.com/henryw374/time-literals

(time-literals.read-write/print-time-literals-cljs!)
#_(cljs.reader/register-tag-parser! time-literals.read-write/tags)
#_(doall (map #(cljs.reader/register-tag-parser! (first %) (second %)) time-literals.read-write/tags))
(cljs.reader/register-tag-parser! 'time/date (time-literals.read-write/tags 'time/date))

#_(clojure.edn/read-string {:readers time-literals.read-write/tags} "#time/date \"2011-01-01\"")
;(clojure.edn/read-string "#time/date \"2011-01-01\"")
;(println (t/date "2011-01-01"))

(comment
  (let [h (t/date "2023-01-02")]
    {:abstract (pr-str h)
     :concrete (binding [*print-dup* true]
                 (pr-str h))})

  (time-literals.read-write/print-time-literals-cljs!)
  (println #time/duration "PT1S")

  ; literal does not work in cljs??                                              (rf/dispatch [:set/data-path path val-t]))}])))
  ; (def d #time/date"2039-01-01")
  (def d (t/date "2039-01-01"))

  (time-literals.read-write/print-time-literals-clj!)
  (map #(cljs.reader/register-tag-parser! (first %) (second %)) time-literals.read-write/tags)

  (cljs.reader/register-tag-parser! 'time/date (time-literals.read-write/tags 'time/date))
  (def d #time/date"2039-01-01")
  (time-literals.read-write/tags 'time/period))


(def time-deserialization-handlers
  (assoc-in time/time-deserialization-handlers
            [:handlers "LocalDate"] (transit/read-handler #(t/date %))))

(def time-serialization-handlers
  (assoc-in time/time-serialization-handlers
            [:handlers LocalDate] (transit/write-handler
                                    (constantly "LocalDate")
                                    #(str %))))

;(belib.browser/js-obj->clj-map time-serialization-handlers)

;(def date (t/date "2023-04-03"))
;(println "TYPE")
;(println (= (type date) java.time.LocalDate))
;(println java.time.LocalDate)

(comment
  (defn transit-out [data]
    (let [w (transit/writer :json time-serialization-handlers)]
      (transit/write w data)))

  (defn transit-in [data]
    (let [r (transit/reader :json time-deserialization-handlers)]
      (transit/read r data)))

  (println "t/date OUT: " (transit-out (t/date "2023-04-03")))
  (transit-out {:x "y"})
  (def date-transit (transit-out (t/date)))
  (def restored-date (transit-in date-transit)))


(rf/reg-event-fx
  :model/save
  (fn [cofx _]
    (println "saving model:")
    ;(pprint (:model (:db cofx))) ;TODO remove load data???
    {:http-xhrio {:method          :post
                  :params          (:model (:db cofx))
                  :uri             "/api/send-recv-date"
                  :format          (ajax/transit-request-format #_ajax/json-request-format
                                     time-serialization-handlers)
                  :response-format (ajax/transit-response-format #_(ajax/raw-response-format {:keywords? true})
                                     time-deserialization-handlers)
                  :on-success      [:authorized-success]
                  :on-failure      [:authorized-failure]}}))

(comment
  (rf/dispatch [:model/save])
  (type (#time/date "2039-01-01")))