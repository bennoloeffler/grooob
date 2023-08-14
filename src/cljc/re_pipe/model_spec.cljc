(ns re-pipe.model-spec
  (:require
    ;[java-time.api :as jt]
    [tick.core :as t]
    [belib.core :as bc]
    [belib.spec :as bs]
    [belib.date-time :as bd]
    ;[belib.cal-week-year :as bcw]
    [clojure.set :as set]
    [clojure.spec.alpha :as s]
    [clojure.test.check.generators :as gen]

    [hyperfiddle.rcf :refer [tests]])
  #_(:import [java.time LocalDate]
      [java.time Period]))


(hyperfiddle.rcf/enable! false)

;;--------------------
;; based on week time model
;;--------------------
;;
;; a linear week model since 2010-01-04 until 2039-12-31 (including).
;; options for handling calender weeks
;; see belib.cal-week/abs-week-map
;; it maps       Date -> [abs-week year week]
;; #inst "2023-04-11" -> [693      2023 15]
;; abs-week starts with 1 at 2010-01-04
;; abs-week ends with 1565 at 2039-12-31


(def example-model
  {:g/name "example-model"
   :g/projects
   {"p1" {:g/entity-id "p1"
          :g/name      "p1"
          :g/end       (bd/d "2022-04-01")
          :g/tasks
          {"tid-1" {:g/entity-id     "tid-1"
                    :g/start         (bd/d "2024-04-01")
                    :g/end           (bd/d "2024-04-20")
                    :g/resource-id   "engineering"
                    :g/capacity-need 15}
           123     {:g/entity-id     123
                    :g/start         (bd/d "2024-04-10")
                    :g/end           (bd/d "2024-04-30")
                    :g/resource-id   "purchasing"
                    :g/capacity-need 10}}}
    "p2" {:g/entity-id "p2"
          :g/name      "p2"
          :g/end       (bd/d "2022-04-01")
          :g/tasks
          {"tid-2" {:g/entity-id     "tid-2"
                    :g/start         (bd/d "2022-04-01")
                    :g/end           (bd/d "2022-04-20")
                    :g/resource-id   "engineering"
                    :g/capacity-need 15}
           1234    {:g/entity-id     1234
                    :g/start         (bd/d "2022-04-10")
                    :g/end           (bd/d "2022-04-30")
                    :g/resource-id   "purchasing"
                    :g/capacity-need 10}}}}

   :g/pipelines
   {123 {:g/entity-id         123
         :g/name              "abc"
         :g/max-ip            12
         ; sequence
         :g/projects-sequence ["p1"]}
    12  {:g/entity-id         123
         :g/name              "abc"
         :g/max-ip            12
         ; sequence
         :g/projects-sequence ["p2"]}}
   :g/resources
   {"engineering" {:g/entity-id   "engineering"
                   :g/name        "Engineering X"
                   :g/norm-capa   {:g/yellow 15 :g/red 25}
                   :g/change-capa [[(bd/d "2022-04-30") {:g/yellow 25 :g/red 35}]
                                   [(bd/d "2022-04-30") {:g/yellow 25 :g/red 35}]]}
    "purchasing"  {:g/entity-id   "engineering"
                   :g/name        "Purchasing 23"
                   :g/norm-capa   {:g/yellow 15 :g/red 25}
                   :g/change-capa [[(bd/d "2022-04-30")
                                    {:g/yellow 25 :g/red 35}]]}
    "assembly"    {:g/entity-id   "assembly"
                   :g/name        "ass 23"
                   :g/norm-capa   {:g/yellow 15 :g/red 25}
                   :g/change-capa [[(bd/d "2022-04-30")
                                    {:g/yellow 25 :g/red 35}]]}}

   :g/load
   {"engineering" {:g/total-load    {693 24 694 26}
                   :g/tasks-details {693 {123  7
                                          1234 2}}}
    "assembly"    {:g/total-load    {}
                   :g/tasks-details {693 {123  7
                                          1234 2}
                                     696 {"tid-1" 70
                                          "tid-2" 20}}}}})

;;--------------------
;;
;; spec the model
;;
;; common specs helper
;;--------------------

;; spec generators local data and others
;; https://gist.github.com/nikolavojicic/8af3679f43d1d3f1bf5393a646be60bf

#_(defn local-date?
    "is ld of type LocalDate?"
    [ld]
    (= LocalDate (type ld)))

;; :g/xxx stands for global
;; Just to avoid huge namespace prefixes in other files

(s/def :g/local-date :belib/local-date)
(s/def :g/start :g/local-date)
(s/def :g/end :g/local-date)

; in examples, use strings for entity-id.
; otherwise, use longs.
#_(s/def :g/entity-id (s/or :string string?
                            :pos-int (s/and int? pos?)))
(s/def :g/entity-id :belib/local-id)
(s/def :g/name string?)

;;----------------
;; task - spec
;;----------------

(def resources-ids-range (map str (range 1 11)))
(def resources-ids-set (set resources-ids-range))
(s/def :g/resource-id (s/with-gen :g/entity-id
                                  #(s/gen resources-ids-set)))
(comment
  (gen/sample (s/gen :g/resource-id)))

(s/def :g/capacity-need (s/with-gen (s/and number? (complement neg?))
                                    #(s/gen (s/int-in 0 1000)))) ; long or double

(defn start-before-end? [task]
  (t/<
    (:g/start task)
    (:g/end task)))

(defn shorter-than-5-years? [task]
  (< (bd/duration-in-days (:g/start task)
                          (:g/end task)) (* 5 365)))

(def sequence-num (atom 0))
(defn next-sequence-num []
  (swap! sequence-num inc))

(defn gen-sequnce-num []
  (gen/fmap (fn [dummy] (next-sequence-num))
            (s/gen pos-int?)))

(s/def :g/sequence-num
  (s/with-gen (s/and int? pos?)
              gen-sequnce-num))

(s/def :g/task (s/and
                 (s/keys :req [:g/entity-id
                               :g/start
                               :g/end
                               :g/resource-id
                               :g/capacity-need
                               :g/sequence-num])
                 shorter-than-5-years?
                 ;#(jt/after? (:g/start %) bcw/first-date)
                 ;#(jt/before? (:g/end %) bcw/last-date)
                 start-before-end?))

;; TODO:
;;  totally remove integration phase

(tests
  (->> (gen/sample (s/gen :g/task) 100)
       (map #(s/valid? :g/task %))
       (filter false?)) := []
  nil)

(tests
  (let [example-task (get-in example-model [:g/projects "p1" :g/tasks 123])]
    (s/valid? :g/task example-task)) := true
  ;(s/explain :g/task example-task))
  nil)


;;----------------
;; load - spec
;;----------------
(s/def :g/abs-week int?)
(s/def :g/total-load (s/map-of :g/abs-week number?))
(s/def :g/details-map (s/map-of :g/entity-id number?))
(s/def :g/tasks-details (s/map-of :g/abs-week :g/details-map))
(s/def :g/one-resource-load (s/keys :req [:g/total-load :g/tasks-details]))
(s/def :g/load (s/map-of :g/resource-id
                         :g/one-resource-load))

(tests
  (s/valid? :g/total-load {693 24 694 26}) := true
  (s/valid? :g/total-load {693 24 694 "26"}) := false
  (comment (s/explain :g/total-load {693 24 694 "26"}))
  (s/valid? :g/tasks-details {693 {}}) := true
  (s/valid? :g/tasks-details {693 {(bc/next-local-id) 7
                                   12345674           2}}) := true
  (s/valid? :g/one-resource-load {:g/total-load    {693 24 694 26}
                                  :g/tasks-details {693 {1 7
                                                         2 2}}}) := true

  (let [load-example (get-in example-model [:g/load])]
    (s/valid? :g/load load-example) := true)
  ;(s/explain :g/load load-example))

  nil)

;;----------------
;; projects - spec
;;-----------------
(def projects-ids-range (vec (map str (range 100 110))))
(def projects-ids-set (set projects-ids-range))
(defn get-rand-project-id [] (rand-nth projects-ids-range))

(s/def :g/tasks (s/map-of :g/entity-id :g/task))
(s/def :g/project (s/keys :req [:g/entity-id ; :g/project-id
                                :g/name
                                :g/end
                                :g/tasks]))
(s/def :g/projects (s/map-of :g/entity-id :g/project))

(tests
  (let [projects (get-in example-model [:g/projects])]
    ;"p2" {} not ok
    (s/valid? :g/projects projects)) := true)

;;----------------
;; resources - spec
;;-----------------
(s/def :g/yellow-red-entry (s/keys :req [:g/yellow :g/red]))
(s/def :g/change-capa-entry (s/tuple :g/local-date :g/yellow-red-entry))
(s/def :g/change-capa (s/coll-of :g/change-capa-entry :into []))
(s/def :g/norm-capa :g/yellow-red-entry)
(s/def :g/resource (s/keys :req [:g/entity-id
                                 :g/name
                                 :g/norm-capa]
                           :opt [:g/change-capa]))
(s/def :g/resources (s/map-of :g/entity-id :g/resource))

(tests
  (let [resources (get-in example-model [:g/resources])]
    (s/valid? :g/resources resources)) := true)

;;----------------
;; pipeline - spec
;;-----------------

(s/def :g/max-ip number?)
; sequence of projects
(s/def :g/projects-sequence (s/coll-of :g/entity-id))
(s/def :g/pipeline (s/keys :req [:g/entity-id
                                 :g/name
                                 :g/max-ip
                                 ; sequence
                                 :g/projects-sequence]))
(s/def :g/pipelines (s/map-of :g/entity-id :g/pipeline))

(tests
  (let [pipelines (get-in example-model [:g/pipelines])]
    (s/valid? :g/pipelines pipelines)) := true)

;;----------------
;; model - spec
;;-----------------

(defn all-tasks [model]
  (->> model
       :g/projects
       vals
       (map #(:g/tasks %))
       (reduce merge {})))

(defn all-task-ids-in-all-projects
  [model] (->> (all-tasks model)
               keys
               set))

(defn all-res-ids-in-all-tasks
  [model] (->> (all-tasks model)
               vals
               (map :g/resource-id)
               set))

(defn all-res-ids-in-resources
  [model] (->> model
               :g/resources
               keys
               set))

(defn all-task-ids-in-load
  [model] (->> model
               :g/load
               vals
               (map :g/tasks-details)
               (map vals)
               flatten
               (map keys)
               flatten
               set))

(defn all-project-ids
  [model] (->> model
               :g/projects
               keys
               set))

(defn all-project-ids-in-pipelines [model]
  (->> model
       :g/pipelines
       vals
       (map :g/projects-sequence)
       flatten
       set))

(defn all-res-ids-in-load
  [model] (->> model
               :g/load
               keys
               set))

; okTODO check keys
; ok projects-id in pipeline need to exist in projects (and vice verca)
; ok resource-id in task need to exist in resources (not vice verca)
; ok resource-id in load need to exist in resources (not vice verca)
; ok task-id in load / task-details need to exist in tasks (and vice verca)
(comment
  (all-tasks example-model)
  (all-task-ids-in-all-projects example-model)
  (all-res-ids-in-all-tasks example-model)
  (all-res-ids-in-resources example-model)
  (all-task-ids-in-load example-model)
  nil)

(defn all-res-ids-in-load-match? [model]
  (let [res-ids-load (all-res-ids-in-load model)
        all-res-ids  (all-res-ids-in-resources model)]
    #_[res-ids-pr all-res-ids]
    (= (set/intersection res-ids-load all-res-ids) res-ids-load)))

(defn all-project-ids-in-pipelines-match? [model]
  (let [all-pr-in-pip (all-project-ids-in-pipelines model)
        all-pr        (all-project-ids model)]
    (= all-pr-in-pip all-pr)))

(defn all-task-ids-in-load-match? [model]
  (let [tasks-ids-pr (all-task-ids-in-all-projects model)
        tasks-ids-lo (all-task-ids-in-load model)]
    (= tasks-ids-pr tasks-ids-lo)))

(defn all-res-ids-in-pr-match? [model]
  (let [res-ids-pr  (all-res-ids-in-all-tasks model)
        all-res-ids (all-res-ids-in-resources model)]
    #_[res-ids-pr all-res-ids]
    (= (set/intersection res-ids-pr all-res-ids) res-ids-pr)))

(s/def :g/model (s/and
                  all-res-ids-in-load-match?
                  all-res-ids-in-pr-match?
                  all-task-ids-in-load-match?
                  all-project-ids-in-pipelines-match?
                  (s/keys :req [:g/name
                                :g/projects
                                :g/pipelines
                                :g/resources
                                :g/load])))

(tests
  (some? (bs/validate :g/model example-model)) := true)



