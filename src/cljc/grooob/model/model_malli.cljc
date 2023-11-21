(ns grooob.model.model-malli
  (:require ;[puget.printer :refer [cprint]] not cljs
    #_#?(:cljs [debux.cs.core :as d :refer-macros [clog clogn dbg dbgn break
                                                   clog_ clogn_ dbg_ dbgn_ break_]]
         :clj  [debux.cs.core :as d :refer [clog clogn dbg dbgn break
                                            clog_ clogn_ dbg_ dbgn_ break_]])
    [playback.core]
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
    [belib.test :as bt]
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

(hyperfiddle.rcf/enable! false)


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
   (fn [{:keys [start end]}] (t/> end start))])

(def task-schema
  [:and
   [:map
    [:id :int]
    [:name :string]
    [:start date-schema]
    [:start-cw {:optional true} [:tuple :int :int :int] #_:any]
    [:end date-schema]
    [:end-cw {:optional true} [:tuple :int :int :int] #_:any]
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
  {:id          1
   :name        "t1"
   :start       (d "2013-01-01")
   :end         (d "2013-01-07")
   :capa-need   20
   :resource-id 1
   :description "short-t-d"})
(def short-task-2
  {:id          2
   :name        "t2"
   :start       (d "2013-01-10")
   :end         (d "2013-01-30")
   :capa-need   10
   :resource-id 2
   :description "short-t2-d"})
(def short-task-3
  {:id          3
   :name        "t3"
   :start       (d "2013-02-01")
   :end         (d "2013-02-07")
   :capa-need   20
   :resource-id 1
   :description "short-t3-d"})
(def short-task-4
  {:id          4
   :name        "t4"
   :start       (d "2013-02-10")
   :end         (d "2013-02-27")
   :capa-need   10
   :resource-id 2
   :description "short-t4-d"})

(tests

  (m/schema? (m/schema task-schema)) := true

  (bm/hum-err task-schema short-task)

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
   [:promised-cw {:optional true} [:tuple :int :int :int]]

   [:start {:optional true} date-schema]
   [:end {:optional true} date-schema]
   [:start-cw {:optional true} [:tuple :int :int :int]]
   [:end-cw {:optional true} [:tuple :int :int :int]]

   ;[:seq-nr :int]
   [:tasks [:vector task-schema]]])

(tests

  (m/schema? (m/schema project-schema)) := true

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

  (m/schema? (m/schema resource-schema)) := true

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
;; ----------- all grooob schemas to registry ----------
;;

(mr/set-default-registry!
  (mr/composite-registry
    (m/default-schemas)
    (met/schemas)
    ;{:time/restricted-local-date date-schema} ; with namespaced keyword
    {:grooob-date date-schema} ; without namespaced keyword is ok!
    {:grooob-task task-schema}
    {:grooob-project project-schema}
    {:grooob-resource resource-schema}
    {:grooob-model model-schema}))

(tests
  ;(bm/hum-err-test :time/restricted-local-date (d "1999-01-01")) := true
  (bm/hum-err-test :grooob-date (d "1999-01-01")) := true
  (bm/hum-err-test :grooob-task short-task) := true

  (bm/hum-err-test-pr :grooob-project {:id       3
                                       :name     "p3"
                                       ;:seq-nr   4
                                       :promised (d "2024-11-30")
                                       :tasks    [short-task short-task]}) := true
  (bm/hum-err-test :grooob-resource {:id   4
                                     :name "r3"
                                     ;:seq-nr 1
                                     :capa {(d "2024-11-30") {:yellow 20 :red 30}}}) := true

  (bm/hum-err-test :grooob-model {:projects  [{:id       5
                                               :name     "p3"
                                               ;:seq-nr   4
                                               :promised (d "2024-11-30")
                                               :tasks    [short-task short-task]}]
                                  :resources [{:id   3
                                               :name "r3"
                                               ;:seq-nr 1
                                               :capa {(d "2024-11-30") {:yellow 20 :red 30}}}]}) := true)




;;
;; ----------- weekify model ----------
;;

(defn weekify-task [t]
  (-> t
      (bd/weekify :start)
      (bd/weekify :end)))

(defn de-weekify-task [t]
  (-> t
      (dissoc :start-cw)
      (dissoc :end-cw)))

(defn weekify-tasks [p]
  (as-> (:tasks p) $
        (mapv #(weekify-task %) $)
        (assoc p :tasks $)))

(defn de-weekify-tasks [p]
  (as-> (:tasks p) $
        (mapv #(de-weekify-task %) $)
        (assoc p :tasks $)))

(defn weekify-projects [m]
  (as-> (:projects m) $
        (mapv #(bd/weekify % :promised) $)
        (mapv #(weekify-tasks %) $)
        (assoc m :projects $)))

(defn de-weekify-projects [m]
  (as-> (:projects m) $
        (mapv #(dissoc % :promised-cw) $)
        (mapv #(dissoc % :start) $)
        (mapv #(dissoc % :start-cw) $)
        (mapv #(dissoc % :end) $)
        (mapv #(dissoc % :end-cw) $)
        (mapv #(de-weekify-tasks %) $)
        (assoc m :projects $)))


;;
;; ----------- start end detection ----------
;;

(defn update-start-end
  "Extends the range of time of :start or :end
  (both tick/date) of m, if date is
  before :start or
  after :end"
  [m date]
  (if-not (and (:start m) (:end m))
    (-> m
        (assoc :start date)
        (assoc :end date)
        (bd/weekify :start)
        (bd/weekify :end))
    (let [start (:start m)
          end   (:end m)]
      (if (t/< date start)
        (-> m
            (assoc :start date)
            (bd/weekify :start))
        (if (t/> date end)
          (-> m
              (assoc :end date)
              (bd/weekify :end))
          m)))))
(tests
  (-> {}
      (update-start-end (d "2023-03-01"))
      (update-start-end (d "2022-01-01")))
  := {:start    (d "2022-01-01"),
      :end      (d "2023-03-01"),
      :start-cw [2713 2021 52],
      :end-cw   [2774 2023 9]}

  :end-tests)

(defn find-start-end
  "Finds the overall start and end date of all elements
  that have to contain :start and :end as tick/date each.
  Writes it as :start and :end to element"
  [element elements]
  (if (and elements (> (count elements) 0))
    (let [assert  (mapv #(do (assert (:start %) (str "an element missing :start " %))
                             (assert (t/date? (:start %)) (str ":start needs to satisfy t/date? " %))
                             (assert (:end %) (str "an element missing :end " %))
                             (assert (t/date? (:end %)) (str ":end needs to satisfy t/date? " %)))
                        elements)
          min-max (fn [min-max-fn start-or-end]
                    (apply min-max-fn
                           (map start-or-end elements)))

          start   (min-max t/min :start)
          end     (min-max t/max :end)]
      (-> element
          (assoc :start start)
          (assoc :end end)
          (bd/weekify :start)
          (bd/weekify :end)))
    element))



(tests
  (find-start-end {} [{:start (d "2023-02-01") :end (d "2024-02-01")}
                      {:start (d "2022-02-01") :end (d "2022-02-01")}])
  := {:start    (d "2022-02-01"),
      :end      (d "2024-02-01"),
      :start-cw [2718 2022 5],
      :end-cw   [2822 2024 5]}

  (find-start-end {} []) := {}

  (-> (find-start-end {} [{:start ""} {:start ""}])
      bt/return-ex
      ex-message
      (subs 0 58))
  := "Assert failed: :start needs to satisfy t/date? {:start \"\"}"

  (-> (find-start-end {} [{:end (d "2023-01-01")}])
      bt/return-ex
      ex-message
      (subs 0 71))
  := "Assert failed: an element missing :start {:end #time/date \"2023-01-01\"}"

  :end-test)

(defn find-start-end-projects [m]
  (as-> m $
        (:projects $)
        (mapv #(find-start-end % (:tasks %)) $)
        (assoc m :projects $)))

(defn create-all-caches [m]
  (as-> m $
        (weekify-projects $)
        (find-start-end-projects $)
        (find-start-end $ (:projects $))
        (reduce (fn [m d] (update-start-end m d))
                $
                (->> $ :projects (map :promised)))))

(defn remove-all-caches [m]
  (as-> m $
        (de-weekify-projects $)
        (dissoc $ :start)
        (dissoc $ :end)
        (dissoc $ :start-cw)
        (dissoc $ :end-cw)))


(def test-model (-> {:projects  [{:id       5
                                  :name     "p5"
                                  :promised (d "2013-02-01")
                                  :tasks    [short-task
                                             short-task-2]}
                                 {:id       6
                                  :name     "p6"
                                  :promised (d "2013-02-27")
                                  :tasks    [short-task-3
                                             short-task-4]}
                                 {:id       7
                                  :name     "p7"
                                  :promised (d "2013-02-01")
                                  :tasks    [short-task-3
                                             short-task-4]}]
                     :resources [{:id   1
                                  :name "r1"
                                  :capa {(d "2024-11-30") {:yellow 20 :red 30}}}
                                 {:id   2
                                  :name "r2"
                                  :capa {(d "2024-11-30") {:yellow 20 :red 30}}}]}))


(def test-model-with-start-end
  (-> test-model
      create-all-caches))

(def decached-model (-> test-model-with-start-end
                        remove-all-caches))


;;
;; ----------   API for view data from domain model   ----------
;;

(defn view-model [model]
  (let [view-projects (mapv (fn [raw-project]
                              (let [start (-> raw-project
                                              :start-cw
                                              first)
                                    end   (-> raw-project
                                              :end-cw
                                              first)]
                                {:point   (-> raw-project :promised-cw first)
                                 :start-x start
                                 :len-x   (max 1 (- end start)) ; not 0!
                                 :name    (:name raw-project)
                                 :id      (:id raw-project)}))
                            (:projects model))]
    {:min-cw   (->> model
                    :start-cw
                    first)
     :max-cw   (->> model
                    :end-cw
                    first)
     :projects view-projects}))

(def test-view-model
  (view-model test-model-with-start-end))

(defn view-current-project
  "gives a model that looks like:
  [idx start-x len-x name id]
  where
  :idx is an index, starting at 0
  :start-x is the beginning of the element
  :len-x is the len of the element
  :name is the name of the element
  :id is the id of the corresponding original element"
  [project model]
  (let [task-fn (fn [task]
                  {:start-x (first (:start-cw task)) ;start
                   :len-x   (- (first (:end-cw task))
                               (first (:start-cw task))) ;len
                   :name    (:description task) #_(get-in model [:resources (:resource-id task) :name])
                   ;:sequence-num (:g/resource-id task)
                   :id      (:id task)})] ; ressource


    ;(println model)
    (when project
      {:name     (:name project)
       :min-cw   (first (:start-cw project))
       :max-cw   (first (:end-cw project))
       :projects (mapv task-fn
                       (:tasks project))})))


;;
;; ----------   API for the model   ----------
;;



(defn move-task [m project-idx task-idx days]
  (if (= days 0)
    m ; do nothing
    (let [direction (if (> days 0)
                      t/>>
                      t/<<)
          days      (Math/abs days)
          moved-m-t (-> m
                        (bd/update-date-in [:projects project-idx :tasks task-idx] :start #(direction % days))
                        (bd/update-date-in [:projects project-idx :tasks task-idx] :end #(direction % days)))
          new-start (get-in moved-m-t [:projects project-idx :tasks task-idx :start])
          new-end   (get-in moved-m-t [:projects project-idx :tasks task-idx :end])
          tasks     (get-in moved-m-t [:projects project-idx :tasks])
          ;project   (get-in moved-m-t [:projects project-idx])
          moved-m-p (-> moved-m-t
                        (update-in [:projects project-idx] find-start-end tasks)
                        (update-start-end new-start)
                        (update-start-end new-end))]

      moved-m-p)))

(tests
  (let [{[{[{start :start start-cw :start-cw}] :tasks}] :projects} (move-task test-model-with-start-end 0 0 14)]
    [start start-cw])
  := [(d "2013-01-15")
      [2246 2013 3]]

  :end-tests)


(defn move-project [m project-idx days]
  (if (= days 0)
    m ; do nothing
    (let [tasks-idx-range (range (count (get-in m [:projects project-idx :tasks])))
          m-moved         (reduce (fn [acc task-idx] (move-task acc project-idx task-idx days)) m tasks-idx-range)]
      m-moved)))

#_(tests
    (move-project test-model-with-start-end 0 30))

(defn new-res [m name capa])
(defn new-proj [m name end-date])
(defn new-task [m proj-id
                start end capa-need res-id comment])
(defn add-load-for-task [task-id])
(defn del-load-for-task [task-id])
