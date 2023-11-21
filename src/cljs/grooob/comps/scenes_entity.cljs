(ns grooob.comps.scenes-entity
  (:require
    [portfolio.reagent :refer-macros [defscene]]
    [clojure.string :as str]
    [grooob.comps.ui]
    [belib.malli :as bm]
    [grooob.model.model-malli :as mm]
    [belib.date-time :as bd]
    [re-frame.core :as rf]
    [tick.core :as t]))



(defscene field-by-field-descriptor
          [grooob.comps.ui/edit-one-field
           {:entities-path [:model :projects]
            :entity-id     0}
           {:field-key :name}
           {:label? true}])

(defscene entity-by-field-descriptor
          [grooob.comps.ui/edit-one-entity
           {:entities-path [:model :projects 0 :tasks]
            :entity-id     0}
           [{:field-key :name}
            {:field-key  :start
             :field-type :date}
            {:field-key  :end
             :field-type :date}
            {:field-key  :capa-need
             :field-type :number}
            {:field-key     :resource-id
             :field-type    "ref"
             :ref-path      [:model :resources]
             :ref-id        :id
             :ref-selectors [:name :id]}
            {:field-key :id
             :disabled  true}]
           {:label? true}])

(defscene all-entities-by-field-descriptor
          [grooob.comps.ui/edit-all-entities
           true
           {:entities-path [:model :projects 0 :tasks]}
           [{:field-key :name}
            {:field-key  :start
             :field-type :date}
            {:field-key  :end
             :field-type :date}
            {:field-key     :capa-need
             :field-show-as "hours needed"
             :field-type    :number}
            {:field-key     :resource-id
             :field-show-as "resource"
             :field-type    "ref"
             :ref-path      [:model :resources]
             :ref-id        :id
             :ref-selectors [:name :id]}
            {:field-key :id
             :disabled  true}]
           {:label? true}])

(defscene all-entities-by-field-descriptor-update-fn
          (let [update-fn (fn [{:keys [full-path value]}]
                            (println "update-fn:  path: " full-path ", value: " value)
                            (rf/dispatch [:set/data-path full-path value]))]
            (fn []
              [grooob.comps.ui/edit-all-entities
               true
               {:entities-path [:model :projects 0 :tasks]}
               [{:field-key :name}

                {:field-key  :start
                 :field-type :date
                 :update-fn  update-fn}

                {:field-key  :capa-need
                 :field-type :number}

                {:field-key :id
                 :disabled  true}]
               {:label? true}])))



(rf/reg-event-db
  :set/date
  (fn [db [_ [entity-path date-key date-value]]]
    (println "in event :set/date : " entity-path date-key date-value)
    (let [d       (t/date date-value)
          db-task (bd/assoc-date-in db entity-path date-key d)]
      db-task)))


(rf/reg-event-db
  :set/update-cache
  (fn [db [_ [p-path date-value]]]
    (println "in event: :set/update-cache" p-path date-value)
    (let [d       (t/date date-value)
          db-proj (update-in db p-path mm/update-start-end d)
          m-path  (vec (-> p-path drop-last drop-last))
          db-mod  (update-in db-proj m-path mm/update-start-end d)]
      db-mod)))


(defscene all-entities-by-field-descriptor-update-fn-date
          (let [update-fn (fn [{:keys [full-path value]}]
                            (let [entity-path (vec (drop-last full-path))
                                  key         (last full-path)]
                              (println "path: " entity-path ", key: " key ", value: " value)
                              (rf/dispatch [:set/date [entity-path key value]])
                              (rf/dispatch [:set/update-cache [(-> entity-path drop-last drop-last) value]])))]
            (fn []
              [grooob.comps.ui/edit-all-entities
               true
               {:entities-path [:model :projects 0 :tasks]}
               [#_{:field-key :name}

                {:field-key  :start
                 :field-type :date
                 :update-fn  update-fn}

                {:field-key  :end
                 :field-type :date
                 :update-fn  update-fn}

                #_{:field-key  :capa-need
                   :field-type :number}

                {:field-key :id
                 :disabled  true}]
               {:label? true}])))