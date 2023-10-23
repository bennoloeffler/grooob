(ns grooob.comps.scenes
  (:require
    [portfolio.reagent :refer-macros [defscene]]
    [clojure.string :as str]
    [grooob.comps.ui]
    [belib.malli :as bm]
    [re-frame.core :as rf]))



(defscene menu
          [grooob.comps.ui/overview-proj-details-menu])

(defscene input-field-simple
          [grooob.comps.ui/input-field (atom {}) :data "text" "Your Input"])


(defscene input-field-icon
          [grooob.comps.ui/input-field (atom {}) :data "text" "Your Input"
           "fa-user"])

(defscene input-field-constrained
          [grooob.comps.ui/input-field (atom {}) :data "text" "Your Input in UPPERCASE" "fa-arrow-down-a-z"
           (fn nil-if-no-errors-vec-of-str-if-errors [data]
             (when (and data
                        (not= data (str/upper-case data)))
               ["please type uppercase!"]))])

(defscene input-field-email
          [grooob.comps.ui/input-field (atom {}) :data "email" "Your Email" "fa-envelope"
           (fn [data] (bm/hum-err bm/email-schema data))])

(defscene password-field
          [grooob.comps.ui/input-password-field (atom {}) :pw "Passwortttt"])

(defscene login-field-effect-bounce
          (let [bounce-off (grooob.comps.ui/fa-bounce-off)]
            [grooob.comps.ui/input-field (atom {}) :data "text" "user" [bounce-off "fa-user"]]))

(defscene login-field-effect-smaller
          (let [smaller (grooob.comps.ui/fa-smaller)]
            [grooob.comps.ui/input-field (atom {}) :data "text" "user" [smaller "fa-user"]]))


(defscene field-by-field-descriptor
          [grooob.comps.ui/new-edit-one-field
           {:entities-path [:model :projects]
            :entity-id     0}
           {:field-key :name}
           {:label? true}])

(defscene entity-by-field-descriptor
          [grooob.comps.ui/new-edit-one-entity
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
          [grooob.comps.ui/new-edit-all-entities
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
                            (println "path: " full-path ", value: " value)
                            (rf/dispatch [:set/data-path full-path value]))]
            (fn []
              [grooob.comps.ui/new-edit-all-entities
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