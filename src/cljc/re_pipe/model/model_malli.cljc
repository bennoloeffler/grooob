(ns re-pipe.model.model-malli
  (:require ;[puget.printer :refer [cprint]] not cljs
    #_#?(:cljs [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn break
                                                   clog_ clogn_ dbg_ dbgn_ break_]]
         :clj  [debux.cs.core :as d :refer [clog clogn dbg dbgn break
                                            clog_ clogn_ dbg_ dbgn_ break_]])
    [belib.date-time :as bd]
    ;[clojure.pprint :refer [pprint]]
    #?@(:cljs [[java.time :refer [LocalDateTime LocalDate]]])
    [malli.core :as m]
    [malli.error :as me]
    [malli.dev.pretty :as pretty]
    [malli.util :as mu]
    [malli.transform :as mt]
    [malli.generator :as mg]
    [malli.provider :as mp]
    [malli.destructure :as md]
    [clojure.test.check.generators :as gen]
    [malli.dot :as mdot]
    [malli.instrument :as mi]
    [malli.experimental :as mx]
    ;[malli.dev :as dev]
    [malli.experimental.time :as met]
    [malli.registry :as mr]
    [tick.core :as t]
    [belib.malli :as bm]
    [hyperfiddle.rcf :refer [tests]]
    #?(:clj  [belib.test :as bt :refer [expect-ex return-ex]]
       :cljs [belib.test :as bt :refer-macros [expect-ex return-ex]]))
  #?(:clj
     (:import [java.time LocalDateTime LocalDate])))


; malli, cool intro
; https://www.metosin.fi/blog/malli

; malli: all examples
; also: auth, swagger API
; https://github.com/metosin/reitit/tree/master/examples

; create EVERYTHING from malli
; https://github.com/dvingo/malli-code-gen/blob/main/thoughts.md

(comment
  (use 'debux.core))

(hyperfiddle.rcf/enable! true)


(def d t/date)

; put time schemas in default registry
(mr/set-default-registry!
  (mr/composite-registry
    (m/default-schemas)
    (met/schemas)))


(def date-schema
  [:time/local-date
   {;:min      bcw/first-date ; 2010-01-04 including
    ;:max      bcw/last-date ; 2039-12-31 including
    :gen/fmap (fn [_]
                (let [start     (.toEpochDay (t/date "2010-01-04" #_bcw/first-date))
                      end       (.toEpochDay (t/date "2039-12-31" #_bcw/last-date))
                      epoch-day (->
                                  (- end start)
                                  rand-int
                                  (+ start))]
                  (t/new-date epoch-day)))
    :error/fn
    (fn [{:keys [value]
          ;[_ {:keys [min max]}] :schema ; WONT WORK. :schema is no map, but a Schema object
          :as   all} _]
      ;(println (-> all :schema m/form (get 1)))
      (let [max (-> all :schema m/form (get 1) :max)
            min (-> all :schema m/form (get 1) :min)]
        (if (t/date? value)
          (str "date " value " should be between " min " (including) and " max " (including)")
          (str value " has wrong type: " (type value) ". Should satisfy tick/date? (cljs: joda local date, clj: LocalDate)"))))}])
(tests
  (bm/hum-err-mult-test
    date-schema
    :pass (d "1814-01-01")
    :pass (d "2010-01-04")
    :pass (d "2039-12-31")
    :pass (d "2240-01-01")
    :fail #inst"2020-01-01"
    :pass #?(:clj  (LocalDate/of 2023 1 1)
             :cljs (LocalDate. 2023 1 1))) := true)


(def end-after-start
  [:fn
   {:error/fn
    (fn end-after-start [{{:keys [start end]} :value} _]
      (str "task: :end = " end " should be bigger than :start = " start))}
   (fn [{:keys [start end]}] (> end start))])

(def task-schema
  [:and
   [:map
    [:id :int]
    [:name :string]
    [:start date-schema]
    [:start-cw {:optional true} :any]
    [:end date-schema]
    [:end-cw {:optional true} :any]
    [:capa-need :int]
    [:resource-id :int]
    [:description :string]]
   end-after-start])

(def long-task
  {:id          1 :name "t"
   :start       (d "0010-01-01") ; very, very long ago
   :end         (d "2013-01-01")
   :capa-need   20
   :resource-id 20
   :description "d"})
(def date-wrong-task
  {:id          1 :name "t"
   :start       (d "2014-01-01")
   :end         (d "2013-01-01")
   :capa-need   20
   :resource-id 20
   :description "d"})
(def short-task
  {:id          1 :name "t"
   :start       (d "2013-01-01")
   :end         (d "2013-01-02")
   :capa-need   20
   :resource-id 20
   :description "d"})

(tests

  (return-ex (m/schema? (m/schema task-schema))) := true

  (bm/hum-err-mult-test
    task-schema
    :pass long-task
    :fail date-wrong-task
    :pass short-task)
  := true)

(def project-schema
  [:map
   [:id :int]
   [:name :string]
   [:promised date-schema]
   ;[:seq-nr :int]
   [:tasks [:vector task-schema]]])

(tests

  (return-ex (m/schema? (m/schema project-schema))) := true

  (bm/hum-err-mult-test
    project-schema
    :pass {:id       2
           :name     "p2"
           :promised (d "2024-11-30")
           :tasks    []}
    :pass {:id       3
           :name     "p3"
           :promised (d "2024-11-30")
           :tasks    [short-task]}) := true)

(def resource-schema
  [:map
   [:id :int]
   [:name :string]
   ;[:seq-nr :int]
   [:capa [:map-of #_week date-schema [:map
                                       [:yellow :int]
                                       [:red :int]]]]])

(tests

  (return-ex (m/schema? (m/schema resource-schema))) := true

  (bm/hum-err-mult-test
    resource-schema
    :pass {:id   4
           :name "r3"
           ;:seq-nr 1
           :capa {(d "2024-11-30") {:yellow 20 :red 30}}}
    :fail {:id   4
           :name "r3"
           ;:seq-nr 1
           :capa {(d "2024-11-30") {:yelow 20 :red 30}}}) := true)

(def model-schema
  [:map
   [:projects [:vector project-schema]]
   [:resources [:vector resource-schema]]])


(tests

  (return-ex (m/schema? (m/schema model-schema))) := true


  (bm/hum-err-mult-test
    model-schema
    :pass {:projects  []
           :resources []}
    :pass {:projects  [{:id       5
                        :name     "p3"
                        ;:seq-nr   4
                        :promised (d "2024-11-30")
                        :tasks    [short-task]}]
           :resources [{:id   3
                        :name "r3"
                        ;:seq-nr 1
                        :capa {(d "2024-11-30") {:yellow 20 :red 30}}}]}
    :fail {:projects  {} ;; WRONG
           :resources [{:id   3
                        :name "r3"
                        ;:seq-nr 1
                        :capa {(d "2024-11-30") {:yellow 20 :red 30}}}
                       {:id     4
                        :name   "r3"
                        :seq-nr 1 ;; WRONG
                        :capa   {13 {:yelow #_WRONG 20 :red 30}}}]}) := true)



;;
;; ----------- all re-pipe schemas to registry ----------
;;

(mr/set-default-registry!
  (mr/composite-registry
    (m/default-schemas)
    (met/schemas)
    ;{:time/restricted-local-date date-schema} ; with namespaced keyword
    {:re-date date-schema} ; without
    {:re-task task-schema}
    {:re-project project-schema}
    {:re-resource resource-schema}
    {:re-model model-schema}))

(tests
  ;(bm/hum-err-test :time/restricted-local-date (d "1999-01-01")) := true
  (bm/hum-err-test :re-date (d "1999-01-01")) := true
  (bm/hum-err-test :re-task short-task) := true

  (bm/hum-err-test-pr :re-project {:id       3
                                   :name     "p3"
                                   ;:seq-nr   4
                                   :promised (d "2024-11-30")
                                   :tasks    [short-task short-task]}) := true
  (bm/hum-err-test :re-resource {:id   4
                                 :name "r3"
                                 ;:seq-nr 1
                                 :capa {(d "2024-11-30") {:yellow 20 :red 30}}}) := true

  (bm/hum-err-test :re-model {:projects  [{:id       5
                                           :name     "p3"
                                           ;:seq-nr   4
                                           :promised (d "2024-11-30")
                                           :tasks    [short-task short-task]}]
                              :resources [{:id   3
                                           :name "r3"
                                           ;:seq-nr 1
                                           :capa {(d "2024-11-30") {:yellow 20 :red 30}}}]}) := true)


(def test-model {:projects  [{:id       5
                              :name     "p3"
                              :promised (d "2024-11-30")
                              :tasks    [short-task]}]
                 :resources [{:id   3
                              :name "r3"
                              :capa {(d "2024-11-30") {:yellow 20 :red 30}}}]})

;; API for the model



(tests
  (bm/hum-err model-schema
              (bd/assoc-date-in test-model
                                [:projects 0 :tasks 0]
                                :start
                                (d "2010-01-06")))
  := nil)

(defn new-res [m name capa])
(defn new-proj [m name end-date])
(defn new-task [m proj-id
                start end capa-need res-id comment])
(defn add-load-for-task [task-id])
(defn del-load-for-task [task-id])
