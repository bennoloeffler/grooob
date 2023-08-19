(ns re-pipe.model
  (:require
    ;[debux.common.macro-specs :as ms]
    [belib.date-time :as bd]
    [tick.core :as t]
    [tick.alpha.interval :as tai]
    [belib.core :as bc]
    [belib.spec :as bs]
    [belib.date-time :as dt]
    [clojure.pprint :refer [pprint]]
    [belib.cal-week-year :as bcw]
    [re-pipe.model-spec :as ms :refer [example-model get-rand-project-id projects-ids-range resources-ids-range next-sequence-num]]
    [clojure.spec.alpha :as s]
    [hyperfiddle.rcf :refer [tests]]
    [clojure.test.check.generators :as gen]
    [playback.core]))



(hyperfiddle.rcf/enable! true)

(def d t/date)
(def dt t/date-time)

;; just to load model.cljc in core.cljs (client, browser js) and in core.clj (server jvm)
(defn now-date-time []
  (str "CALLED now-date-time in re-pipe.model at: " (t/instant)))

(comment
  (now-date-time))

(def test-model {:g/name "example-model"
                 :g/projects
                 {"p1" {:g/entity-id "p1"
                        :g/name      "p1"
                        :g/end       (d "2022-04-01")
                        :g/tasks
                        {"tid-1" {:g/entity-id     "tid-1"
                                  :g/start         (d "2024-04-01")
                                  :g/end           (d "2024-04-20")
                                  :g/resource-id   "engineering"
                                  :g/capacity-need 15}
                         123     {:g/entity-id     123
                                  :g/start         (d "2024-04-10")
                                  :g/end           (d "2024-04-30")
                                  :g/resource-id   "purchasing"
                                  :g/capacity-need 10}}}
                  "p2" {:g/entity-id "p2"
                        :g/name      "p2"
                        :g/end       (d "2022-04-01")
                        :g/tasks
                        {"tid-2" {:g/entity-id     "tid-2"
                                  :g/start         (d "2022-04-01")
                                  :g/end           (d "2022-04-20")
                                  :g/resource-id   "engineering"
                                  :g/capacity-need 15}
                         1234    {:g/entity-id     1234
                                  :g/start         (d "2022-04-10")
                                  :g/end           (d "2022-04-30")
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
                                 :g/change-capa [[(d "2022-04-30") {:g/yellow 25 :g/red 35}]
                                                 [(d "2022-04-30") {:g/yellow 25 :g/red 35}]]}
                  "purchasing"  {:g/entity-id   "engineering"
                                 :g/name        "Purchasing 23"
                                 :g/norm-capa   {:g/yellow 15 :g/red 25}
                                 :g/change-capa [[(d "2022-04-30")
                                                  {:g/yellow 25 :g/red 35}]]}
                  "assembly"    {:g/entity-id   "assembly"
                                 :g/name        "ass 23"
                                 :g/norm-capa   {:g/yellow 15 :g/red 25}
                                 :g/change-capa [[(d "2022-04-30")
                                                  {:g/yellow 25 :g/red 35}]]}}

                 :g/load
                 {"engineering" {:g/total-load    {693 24.0 694 26.0}
                                 :g/tasks-details {693 {123  7
                                                        1234 2}}}
                  "assembly"    {:g/total-load    {}
                                 :g/tasks-details {693 {123  7
                                                        1234 2}
                                                   696 {"tid-1" 70
                                                        "tid-2" 20}}}}})

;;-------------------------
;; API
;;-------------------------

(defn new-model [name]
  {:post [(bs/validate :g/model %)]}
  {:g/name      name
   :g/projects  {}
   :g/pipelines {}
   :g/resources {}
   :g/load      {}})

(tests
  (some? (new-model "a-model")) := true)

(defn add-resource
  [model resource-name yellow-limit red-limit]
  {:post [(bs/validate :g/model %)]}
  (assoc-in model [:g/resources resource-name] {:g/entity-id    resource-name
                                                :g/name         resource-name
                                                :g/sequence-num (ms/next-sequence-num)
                                                :g/norm-capa    {:g/yellow yellow-limit :g/red red-limit}
                                                :g/change-capa  []}))

#_(defn add-pipeline
    [model pipeline-name max-ip]
    {:post [(bs/validate :g/model %)]}
    (assoc-in model [:g/pipelines pipeline-name] {:g/entity-id         pipeline-name
                                                  :g/name              pipeline-name
                                                  :g/max-ip            max-ip
                                                  :g/projects-sequence []}))

#_(tests
    (some? (add-pipeline
             (new-model "a-model")
             "pip"
             20)) := true)

; TODO move to belib core
(defn sorted-map-by-key [m sort-by-key]
  (into (sorted-map-by (fn [key1 key2]
                         (- (compare [(get-in m [key2 sort-by-key]) key2]
                                     [(get-in m [key1 sort-by-key]) key1]))))
        m))

(comment
  (type (sorted-map-by-key {} :nix))
  (sorted-map-by-key {:A {:g/sequence-num 1 :val 23}
                      :B {:g/sequence-num 2 :val 22}
                      :C {:g/sequence-num 2 :val 53}
                      :D {:g/sequence-num 5 :val 73}
                      :E {:g/sequence-num 1 :val 10}
                      :F {:g/sequence-num 1 :val 11}}
                     :g/sequence-num)
  (let [results {:A {:sequence-num 1 :val 23}
                 :B {:sequence-num 2 :val 22}
                 :C {:sequence-num 2 :val 53}
                 :D {:sequence-num 5 :val 73}
                 :E {:sequence-num 1 :val 10}
                 :F {:sequence-num 1 :val 11}}]
    (into (sorted-map-by (fn [key1 key2]
                           (compare [(get-in results [key2 :sequence-num]) key2]
                                    [(get-in results [key1 :sequence-num]) key1])))


          results)))

(defn add-project
  [model project-name end]
  #_{:pre  [((fn pipeline-exists [] (get-in model [:g/pipelines pipeline])))]
     :post [(bs/validate :g/model %)]}

  (-> model
      (assoc-in [:g/projects project-name] {:g/entity-id    project-name
                                            :g/name         project-name #_(str "pro-" project-name)
                                            :g/end          (if end end (d "2024-01-01"))
                                            :g/sequence-num (next-sequence-num)
                                            :g/tasks        {}})
      #_(update-in [:g/pipelines pipeline :g/projects-sequence] conj project-name)))

#_(comment
    (-> example-model
        (update-in [:g/pipelines 123 :g/projects-sequence] conj "project-name")))

(tests
  (some? (-> (new-model "a-model")
             (add-resource "engineering" 20 40))) := true)

(tests
  (count (:g/projects (-> (new-model "a-model")
                          (add-resource "engineering" 20 40)
                          #_(add-pipeline "pip-25" 25)
                          (add-project "new-proj" nil)))) := 1)




(defn new-task [task-id start end resource-id capacity-need]
  (let [t {:g/entity-id     task-id
           :g/start         start
           :g/end           end
           :g/resource-id   resource-id
           :g/capacity-need capacity-need}]
    #_(bs/validate :g/task t)
    t))

(defn t [start end resource-id capacity-need]
  (new-task (bc/next-local-id) start end resource-id capacity-need))

(tests
  (count (t (d "2023-05-04")
            (d "2023-05-05")
            "RK" ; change to keyword and see spec fail?
            20)) := 5
  nil)

(defn split-task-to-weeks
  "Divide the capacity demand by days,
  eg. 20 capacity, 10 days, so 2 per day.
  In a task from 2023-04-11 to 2023-04-21,
  this is week 693 amd 694 with 6 and 4 days.
  Every week get's its containing days capacity added.
  That is: {693 12, 694 8}"
  [task abs-week-map]
  #_{:pre  [(bs/validate :g/task task)]
     :post [(bs/validate :g/load %)]}
  (let [all-days          (dt/list-of-all-days (:g/start task) (:g/end task))
        capa-per-day      (/ (:g/capacity-need task) (count all-days))
        days-in-weeks     (map #(first (abs-week-map %)) all-days)
        days-sum-in-weeks (frequencies days-in-weeks)
        weeks-capa-total  (bc/map-kv days-sum-in-weeks #(* % capa-per-day))
        weeks-capa-tasks  (bc/map-kv days-sum-in-weeks #(hash-map (:g/entity-id task) (* % capa-per-day)))]
    {(:g/resource-id task) {:g/total-load    weeks-capa-total
                            :g/tasks-details weeks-capa-tasks}}))

(tests
  (let [task (t (d "2023-04-11") (d "2023-04-21") ":engineering" 20)
        load (split-task-to-weeks task bcw/abs-week-map)]
    (s/valid? :g/load load)) := true)

(comment
  (def task (t (d "2023-04-11") (d "2023-04-21") ":engineering" 20))
  (def split (split-task-to-weeks task bcw/abs-week-map))
  (s/valid? :g/load split)
  (bc/deep-merge-with + {":engineering" {:total {693 18, 751 3, 694 22}
                                         :tasks {693 {:taskid 9}}}
                         ":purchasing"  {:total {693 9, 751 3}
                                         :tasks {693 {:taskid 9}}}} (update-in split [":engineering"] dissoc :tasks) #_{:eng {693 12, 694 8}})
  (bc/deep-merge-with merge {":engineering" {:total {693 18, 751 3, 694 22}
                                             :tasks {693 {:taskid 9}}}
                             ":purchasing"  {:total {693 9, 751 3}
                                             :tasks {693 {:taskid 9}}}} (update-in split [":engineering"] dissoc :total) #_{:eng {693 12, 694 8}}))




;;----------------------------------------------
;; a view to the projects and the model: start and end in date
;;----------------------------------------------
; 0. there is a update-project-start-end functions, that is called on every project upon init.
; 1. then, all projects have :start-end-project [#time/date"2024-04-01" #time/date"2024-04-30"]
; 2. there is a update-model-start-end function, that is called from update-project-start-end.
; 3. update-model-start-end ony EXTENDS the range of start and end. never shrinks!
; RESULT after init: every project knows its start and end date
;                    the model knows its start and end date
; ADDING or REMOVING a task may trigger a change:


; 3. afterwards, the model knows is's start and end in cal weeks.
; 4. when a task is added (not removed), the
;    - minimum of all start is set to project-start-date
;    - maximum of all end is set to project-end-date
;    that way, start and end are keept up to date after every task change.
; 5. whenever a task is added, its start and end are checked to extend the current start and end of the model.
; 6. visible area never shrinks during runtime - only at init.
;    That way, the model may indicate more space than needed - but never too less.
; 7. the visible week may be calculated by task-week - start-week
; 8. start-date and end-date of the model are extended by:
; 9. extend-date-range [current-range date] -->returns: [potentially-extended-range]


(defn update-model-start-end [m date]
  (if-not (:g/start-end-model m)
    (assoc m :g/start-end-model [date date])
    (let [start-end (:g/start-end-model m)
          start     (first start-end)
          end       (second start-end)]
      (if (t/< date start)
        (do
          ;(println "moved start: " date)
          (-> m (assoc :g/start-end-model [date end])
              (bcw/weekify-element :g/start-end-model)))
        (if (t/> date end)
          (do
            ;(println "moved end: " date)
            (-> m (assoc :g/start-end-model [start date])
                (bcw/weekify-element :g/start-end-model)))
          m)))))

(defn update-project-start-end
  "Finds the start and end date of projects tasks.
  Writes it in a cache, e.g.:
  :g/start-end-project [#time/date\"2022-04-01\" #time/date\"2022-04-30\"] "
  [m project-name]
  (let [min-max    (fn [min-max-fn g-start-or-end]
                     (apply min-max-fn
                            (map g-start-or-end
                                 (-> m
                                     :g/projects
                                     (get project-name)
                                     :g/tasks
                                     vals))))
        start-of-p (min-max t/min :g/start)
        end-of-p   (min-max t/max :g/end)]
    (-> m
        (assoc-in [:g/projects project-name :g/start-end-project]
                  [start-of-p end-of-p])
        (update-in [:g/projects project-name] bcw/weekify-element :g/start-end-project)
        (update-model-start-end start-of-p)
        (update-model-start-end end-of-p))))



(tests

  (-> test-model
      :g/projects
      (get "p1")
      :g/start-end-project) := nil

  (-> test-model
      (update-project-start-end "p1")
      :g/projects
      (get "p1")
      :g/start-end-project) := [(d "2024-04-01") (d "2024-04-30")])

(comment
  (apply t/max (map :g/start (-> test-model
                                 :g/projects
                                 (get "p2")
                                 :g/tasks
                                 vals)))

  (apply min [(d "2023-01-01") (d "2022-01-01")])
  (assoc-in example-model [:g/projects "p2" :g/some-other] [:a :b])
  (update-project-start-end example-model "p2"))

(defn- init-all-projects-start-end
  "NOT NEEDED, if you build the model with the API.
  Call it for test models in order to calc start and ends."
  [m]
  (let [projects (keys (:g/projects m))]
    (reduce update-project-start-end
            m
            projects)))

(comment
  (reduce update-project-start-end example-model (keys (:g/projects example-model))))

(tests
  (->> (init-all-projects-start-end test-model)
       :g/projects
       vals
       (map :g/start-end-project)
       (filter #(= 2 (count %))) ; is a vector
       (filter #(and (bd/date? (first %)) ; has two dates
                     (bd/date? (second %))))
       (filter #(t/<= (first %) (second %))) ; first smaller or equal than second
       count) := 2) ; two projects are evaluated



;;------------------------------------------------------------------------------
;; add task, remove task, move task
;;-------------------------------------------------------------------------------


(defn add-task
  "Adds a task to model. This means, that the task is
  added to the project and also to the load."
  [model project-id task]
  #_{:pre  [(bs/validate :g/model model)]
     :post [(bs/validate :g/model model)]}
  (let [resource-id        (:g/resource-id task)
        capa-in-weeks      (split-task-to-weeks task bcw/abs-week-map)
        capa-without-tasks (update-in capa-in-weeks [resource-id] dissoc :g/tasks-details)
        capa-without-total (update-in capa-in-weeks [resource-id] dissoc :g/total-load)

        load-total         (bc/deep-merge-with + (:g/load model) capa-without-tasks)
        load               (bc/deep-merge-with merge load-total capa-without-total)
        model-with-load    (assoc model :g/load load)
        model-with-task    (assoc-in model-with-load [:g/projects project-id :g/tasks (:g/entity-id task)] task)
        model-with-task    (update-in model-with-task [:g/projects project-id :g/tasks (:g/entity-id task)] bcw/weekify-element :g/start)
        model-with-task    (update-in model-with-task [:g/projects project-id :g/tasks (:g/entity-id task)] bcw/weekify-element :g/end)]
    (-> model-with-task
        (update-project-start-end project-id))))

(tests
  (let [task     (t (d "2023-04-11") (d "2023-04-21") "engineering" 10)
        task-id  (:g/entity-id task)
        ex-added (add-task test-model "p1" task)
        ;_ (bc/validate :g/model ex-added)
        load-eng (get-in ex-added [:g/load "engineering"])]
    ;; the capa is added in load
    (get-in load-eng [:g/total-load 693]) := 30.0
    ;; the task is aded in project
    (:g/entity-id (get-in ex-added [:g/projects "p1" :g/tasks task-id])) := task-id
    (:g/start-end-model ex-added) := [(d "2023-04-11") (d "2024-04-30")]))


(tests
  (-> (new-model "a-model")
      (add-resource "engineering" 20 40)
      #_(add-pipeline "pip-25" 25)
      (add-project "new-proj" nil #_"pip-25")
      (add-task "new-proj" (t (d "2023-05-07")
                              (d "2023-06-01")
                              "engineering"
                              200))
      :g/projects
      (get "new-proj")
      :g/tasks
      count) := 1)

(comment
  (-> (new-model "a-model")
      (add-resource "engineering" 20 40)
      #_(add-pipeline "pip-25" 25)
      (add-project "new-proj" nil #_"pip-25")
      (add-task "new-proj" (t (d "2023-05-07")
                              (d "2023-06-01")
                              "engineering"
                              200)))

  {:projects {"p1" {:id    "p1"
                    :tasks {"t1" {:id "t1" :start (d "2023-05-01") :end (d "2023-06-04") :resource-id "engineering" :capa-need 100}
                            "t2" {:id "t2" :start (d "2023-05-10") :end (d "2023-06-14") :resource-id "purchasing" :capa-need 100}}}
              "p2" {:id    "p2"
                    :tasks {"t3" {:id "t3" :start (d "2023-05-01") :end (d "2023-06-04") :resource-id "engineering" :capa-need 10}
                            "t4" {:id "t4" :start (d "2023-05-10") :end (d "2023-06-14") :resource-id "purchasing" :capa-need 50}}}}})


#_(comment
    (s/explain (s/or :int int? :str string?) 7.0)

    (repl)
    (pprint example-model)
    (source doc)
    (dir-here)
    (javadoc 2)
    (bean 3)
    (dir-here)
    (print-table [{:a \x :b :false} {:a \c :b 1}])
    (s/check-asserts?)
    (s/check-asserts true)
    (s/assert local-date? (java.util.Date.))
    (s/check-asserts false)
    (s/assert local-date? (java.util.Date.))

    (println "assert active?" *assert*)
    (ea/assert 2)

    (def task (t (d "2023-04-11") (d "2023-04-21") "engineering" 10))
    (def ex-added (add-task example-model "p1" task))
    (bc/validate :g/model ex-added)
    (clojure.inspector/inspect-tree example-model)
    (insp/inspect-tree example-model)
    (cprint example-model)

    (require '[portal.api :as p])
    (add-tap #'p/submit)
    (def p (p/open {:launcher :intellij}))
    (tap> example-model)


    (remove-task example-model :p1 :task-id)

    (pr-str (java.util.Date.)))

(defn remove-task-from-task-details
  "Removes a task from a model and a project.
  This means, that the task is
  removed also from the load."
  [model resource-id task-id]
  (let [details (update-in model [:g/load resource-id :g/tasks-details]
                           (fn [task-details]
                             (bc/map-kv task-details
                                        #(dissoc % task-id))))
        all-ids (keys (get-in model [:g/load resource-id :g/tasks-details]))
        del-ids (filter #(= {} (get-in details [:g/load resource-id :g/tasks-details %])) all-ids)
        details (update-in details [:g/load resource-id :g/tasks-details] #(apply dissoc % del-ids))]

    details))

(defn remove-task-return-it
  "Removes a task from model. This means, that the task is
   removed from the project and also from the load.
   Returns a vector of the removed task and the model without the task."
  [model project-id task-id]
  #_{:pre  [(bs/validate :g/model model)]
     :post [(bs/validate :g/model model)]}
  (let [task               (get-in model [:g/projects project-id :g/tasks task-id])
        model              (bc/dissoc-in model [:g/projects project-id :g/tasks] task-id)
        resource-id        (:g/resource-id task)
        capa-in-weeks      (split-task-to-weeks task bcw/abs-week-map)
        capa-without-tasks (update-in capa-in-weeks [resource-id] dissoc :g/tasks-details)
        total-load         (bc/deep-merge-with - (:g/load model) capa-without-tasks)
        model-with-total   (assoc-in model [:g/load] total-load)
        model-with-details (remove-task-from-task-details model-with-total resource-id (:g/entity-id task))]
    [task model-with-details]))

(defn remove-task
  [model project-id task-id]
  (second (remove-task-return-it model project-id task-id)))

(comment
  (def rem-ex (remove-task example-model "p1" 123)))

#_(tests ;; does not work because of :g/start-end-project and -model
    (let [tid  "tid-3"
          pid  "p1"
          task (new-task tid (d "2023-04-10") (d "2023-04-21") "engineering" 10000)
          em2  (add-task test-model pid task)
          em3  (remove-task em2 pid tid)]
      (= test-model em3) := true))

;(cprint (:g/load em))
;(cprint (:g/load em2))
;(cprint (:g/load em3))

#_(require '[debux.core :refer [dbg dbgn]]
           '[erdos.assert :as ea])

; TODO is not sequence stable!
(defn move-task [m project-id task-id days]
  (if (= days 0)
    m ; do nothing
    (let [[t m] (remove-task-return-it m project-id task-id)
          direction (if (> days 0)
                      t/>>
                      t/<<)
          days      (Math/abs days)
          t         (update t :g/start #(direction % days))
          t         (update t :g/end #(direction % days))]
      (-> m
          (add-task project-id t)
          (update-project-start-end project-id)))))

; DOES NOT WORK
#_(defn move-task-stable [m project-id task-id days]
    (if (= days 0)
      m ; do nothing
      (let [tasks   (keys (get-in m [:g/projects project-id :g/tasks]))
            m-moved (reduce
                      (fn [acc val]
                        (move-task
                          acc
                          project-id
                          val (if (= val task-id)
                                days
                                0)))
                      m tasks)]

        ;(pprint (view-model m-moved))
        m-moved)))

(declare view-model)
(defn move-project [m project-id days]
  (if (= days 0)
    m ; do nothing
    (let [tasks   (keys (get-in m [:g/projects project-id :g/tasks]))
          m-moved (reduce (fn [acc val] (move-task acc project-id val days)) m tasks)]

      ;(pprint (view-model m-moved))
      m-moved)))

(comment
  (move-project (-> (new-model "a-model")
                    (add-resource "engineering" 20 40)
                    #_(add-pipeline "pip-25" 25)
                    (add-project "new-proj-1" nil #_"pip-25")
                    (add-project "new-proj-2" nil #_"pip-25")
                    (add-task "new-proj-1" (t (d "2023-05-07")
                                              (d "2023-06-01")
                                              "engineering"
                                              200))
                    (add-task "new-proj-2" (t (d "2023-05-07")
                                              (d "2023-06-01")
                                              "engineering"
                                              200)))
                "new-proj-1"
                7))

(comment
  (let [result (+ 1 2 3)])
  (reduce (fn [acc val] (println "v" val "a" acc) (+ val acc)) -10 [1 2 3 4 5]))

;;----------------------------------------------
;; Performance test
;;----------------------------------------------

(defn add-project-red-fn [m num]
  (add-project m num bcw/last-date #_"p"))

(defn add-resource-red-fn [m num]
  (add-resource m num 100 200))

(defn add-task-red-fn [m task]
  (add-task m (get-rand-project-id) task))

(defn shorten-task [task]
  (let [end (t/>> (:g/start task)
                  (+ 10 (rand-int 20)))]
    (assoc task :g/end end)))

(defn get-rand-project-task-id [m]
  (let [project-id (rand-nth (keys (:g/projects m)))
        task-id    (-> m
                       :g/projects
                       (get project-id)
                       :g/tasks
                       keys
                       rand-nth)]
    [project-id task-id]))

(defn generate-random-model [num-of-tasks]
  (let [_     (println "creating " num-of-tasks " tasks... takes a while...")
        tasks (time (->> (gen/sample (s/gen :g/task) num-of-tasks)
                         (map #(shorten-task %))))
        ;_               (pprint tasks)
        _     (println "wiring model...")
        m     (time (as-> (new-model "m") $
                          #_(add-pipeline $ "p" 100)
                          (reduce add-project-red-fn $ projects-ids-range)
                          (reduce add-resource-red-fn $ resources-ids-range)
                          (reduce add-task-red-fn $ tasks)))]
    m))


(defn test-move-performance []
  (let [num-of-tasks    200
        m               (generate-random-model num-of-tasks)
        project-task-id (get-rand-project-task-id m)
        pid             (first project-task-id)
        tid             (second project-task-id)
        _               (println "validating model...")
        _               (time (s/valid? :g/model m))
        num-of-moves    10000
        _               (println "now moving task " num-of-moves "times")
        m-moved         (time (doall (reduce (fn [m _] (move-task m pid tid 1)) m (range num-of-moves))))
        _               (println "now validating the model")
        _               (time (s/valid? :g/model m-moved))]))

(comment
  (def m (generate-random-model 5000))
  (s/valid? :g/model m)
  (require '[portal.api :as p])
  ;; OPEN portal tab at right side first
  (def p (p/open {:launcher :intellij}))
  (add-tap #'p/submit)
  (tap> m)

  (test-move-performance))


;;-----------------------------------------------
;; API to the "PROJECT-VIEW-MODEL", which is an adapter
;;-----------------------------------------------
;;
;; the calendar-weeks are mapped to an idx
;; that starts at 0 and has a max.
;; idx = 0 is the same as the start of cw
;; a model has a sequence of projects.
;; a project has a name, a start-cw and a len-cw

(defn view-model [model]
  (let [vals-of-projects (map #(conj (vec (:g/start-end-project-cw %))
                                     (:g/name %)
                                     (:g/entity-id %))
                              (vals (:g/projects model)))
        projects         (map-indexed (fn [idx e] (let [e     e
                                                        start (-> e first first)
                                                        end   (first (second e))]
                                                    {:idx idx :start-x start :len-x (- end start) :name (last e) :id (last e)}))
                                      vals-of-projects)]
    {:min-cw   (->> model
                    :g/start-end-model-cw
                    first
                    first)
     :max-cw   (->> model
                    :g/start-end-model-cw
                    second
                    first)
     :projects projects}))



(defn current-project
  "gives a model that looks like:
  [idx start-x len-x name id]
  where
  :idx is an index, starting at 0
  :start-x is the beginning of the element
  :len-x is the len of the element
  :name is the name of the element
  :id is the id of the corresponding original element"
  [project]
  (let [;project (sorted-map-by-sequence project)
        task-fn (fn [idx task]
                  {:idx          idx ; TODO check, if id of task would be better
                   :start-x      (first (:g/start-cw task)) ;start
                   :len-x        (- (first (:g/end-cw task))
                                    (first (:g/start-cw task))) ;len
                   :name         (:g/resource-id task)
                   :sequence-num (:g/sequence-num task)
                   :id           (:g/entity-id task)})] ; ressource
    (when project
      {:name     (:g/name project)
       :min-cw   (first (first (:g/start-end-project-cw project)))
       :max-cw   (first (second (:g/start-end-project-cw project)))
       :projects (vec (sort-by :sequence-num (map-indexed task-fn
                                                          (vals (:g/tasks project)))))
       #_(vec (sort-by :sequence-num (map-indexed task-fn
                                                  (vals (:g/tasks project)))))})))
;:original project}))

(comment
  (let [{:person/keys [ant]} {:person/ant 17}]
    (println ant))
  (def m {:g/name               "a-model",
          :g/projects           {"new-proj" {:g/entity-id            "new-proj",
                                             :g/name                 "new-proj",
                                             :g/end                  (d "2024-01-01")
                                             :g/tasks                {1688830347370 {:g/entity-id     1688830347370,
                                                                                     :g/start         (d "2023-05-07")
                                                                                     :g/end           (d "2023-06-01")
                                                                                     :g/resource-id   "engineering",
                                                                                     :g/capacity-need 200,
                                                                                     :g/start-cw      [696 2023 18],
                                                                                     :g/end-cw        [700 2023 22]}},
                                             :g/start-end-project    [(d "2023-05-07") (d "2023-06-01")],
                                             :g/start-end-project-cw '([696 2023 18] [700 2023 22])}},
          :g/pipelines          {"pip-25" {:g/entity-id "pip-25", :g/name "pip-25", :g/max-ip 25, :g/projects-sequence ["new-proj"]}},
          :g/resources          {"engineering" {:g/entity-id   "engineering",
                                                :g/name        "engineering",
                                                :g/norm-capa   {:g/yellow 20, :g/red 40},
                                                :g/change-capa []}},
          :g/load               {"engineering" {:g/total-load    {696 8, 697 56, 698 56, 699 56, 700 24},
                                                :g/tasks-details {696 {1688830347370 8},
                                                                  697 {1688830347370 56},
                                                                  698 {1688830347370 56},
                                                                  699 {1688830347370 56},
                                                                  700 {1688830347370 24}}}},
          :g/start-end-model    [(d "2023-05-07") (d "2023-06-01")],
          :g/start-end-model-cw '([696 2023 18] [700 2023 22])})

  (def m-big (-> (new-model "a-model")
                 (add-resource "engineering" 20 40)
                 #_(add-pipeline "pip-25" 25)
                 (add-project "new-proj-1" nil #_"pip-25")
                 (add-project "new-proj-2" nil #_"pip-25")
                 (add-task "new-proj-1" (t (d "2023-05-07")
                                           (d "2023-06-01")
                                           "engineering"
                                           200))
                 (add-task "new-proj-2" (t (d "2024-05-07")
                                           (d "2024-06-18")
                                           "engineering"
                                           200))))

  (map-indexed (fn [i e] (let [start (-> e first first)
                               end   (first (second e))]
                           [i start (- end start) (last e)]))
               (map #(conj (vec (:g/start-end-project-cw %)) (:g/entity-id %))
                    (vals (:g/projects m-big)))))

(tests
  (view-model
    (-> (new-model "a-model")
        (add-resource "engineering" 20 40)
        #_(add-pipeline "pip-25" 25)
        (add-project "new-proj-1" nil #_"pip-25")
        (add-project "new-proj-2" nil #_"pip-25")
        (add-task "new-proj-1" (t (d "2023-05-07")
                                  (d "2023-06-01")
                                  "engineering"
                                  200))
        (add-task "new-proj-2" (t (d "2023-05-07")
                                  (d "2023-06-01")
                                  "engineering"
                                  200)))) := {:min-cw   696
                                              :max-cw   700
                                              :projects [{:idx 0 :start-x 696 :len-x 4 :name "new-proj-1" :id "new-proj-1"}
                                                         {:idx 1 :start-x 696 :len-x 4 :name "new-proj-2" :id "new-proj-2"}]}

  nil)
;:g/projects
;(get "new-proj")
;:g/tasks
;count) := 1)

(comment

  (t/>> (d "2023-01-01") 5)
  (t/- (t/new-duration 5 :days) (t/new-duration 5 :minutes))
  (t/<< (t/date-time "2023-02-01T00:00") (t/new-duration 5 :minutes))
  (t/>> (t/date-time "2023-02-01T00:00") (t/new-duration 5 :minutes))
  (t/<< (t/date-time "2023-02-01T00:00") (t/date-time "2023-02-01T00:00"))


  (shorten-task (t (d "2023-01-01") (d "2024-01-01") "r1" 200))
  (ms/start-before-end? (t (d "2023-01-01") (d "2024-01-01") "r1" 200))

  (t/> (d "2025-01-01") (d "2024-01-01"))
  (t/<< (d "2023-01-01") 5)
  (dec (t/days (t/duration (tai/new-interval (d "2023-01-01") (d "2023-01-02")))))
  (t/between (d "2023-01-01") (d "2024-01-01"))
  (t/duration)

  (def tasks (->> (gen/sample (s/gen :g/task) 100)
                  (map #(shorten-task %))))


  (time (def m (as-> (new-model "m") $
                     #_(add-pipeline $ "p" 100)
                     (reduce add-project-red-fn $ projects-ids-range)
                     (reduce add-resource-red-fn $ resources-ids-range)
                     (reduce add-task-red-fn $ tasks))))


  (def project-task-id (get-rand-project-task-id m))
  (def pid (first project-task-id))
  (def tid (second project-task-id))

  (s/valid? :g/model m)
  (def m-moved (time (doall (reduce (fn [m _] (move-task m pid tid 1)) m (range 1000)))))
  (s/valid? :g/model m-moved)
  (repl)
  (insp/inspect-tree m-moved)

  (require '[portal.api :as p])
  (add-tap #'p/submit)
  (def p (p/open {:launcher :intellij}))
  (tap> m-moved)

  (require '[clj-async-profiler.core :as prof])
  (prof/profile (do
                  (reduce (fn [m _] (move-task m pid tid 1)) m (range 10))
                  nil))
  (prof/serve-ui 8080)

  nil)


