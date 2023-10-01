(ns re-pipe.re-comps.ui
  (:require [re-frame.core :as rf]
            [belib.core :as bc] ; also bc/pprint
            [tick.core :as t]
            [re-pipe.time-transit]))





(defn overview-proj-details-menu []
  (let [_page (rf/subscribe [:common/page-id])]
    (fn []
      ;(println "page: " @_page)
      [:div.tabs ;.is-small ;.is-centered.is-boxed
       [:ul
        [:li {:class (when (= @_page :projects-portfolio) :is-active)} #_(bh/cs (when (= @_page :projects-portfolio) :is-active))
         [:a {:on-click #(rf/dispatch [:common/navigate! :projects-portfolio nil nil])} "Overview"]]
        [:li {:class (when (= @_page :project) :is-active)}
         [:a {:on-click #(rf/dispatch [:common/navigate! :project nil nil])} "Project"]]
        [:li {:class (when (= @_page :project-details) :is-active)}
         [:a {:on-click #(rf/dispatch [:common/navigate! :project-details nil nil])} "Details"]]
        [:li {:class (when (= @_page :raw-data) :is-active)}
         [:a {:on-click #(rf/dispatch [:common/navigate! :raw-data nil nil])} "Raw"]]]])))





(defn edit-form->val
  "transforms the value in its edit form (mostly string)
  in the html browser editor back to the value
  in its data form."
  [edit-form
   {:keys [field-type
           field-key
           ref-path
           ref-id
           ref-selectors]
    :as   field-descriptor}]
  (case field-type
    "number" (long edit-form)
    "date" (t/date edit-form)
    "ref" (ex-info "don't use for refs" {:val              edit-form
                                         :field-descriptor field-descriptor})
    ;"text" edit-form
    edit-form))

(defn val->edit-form
  "transforms a value from the data model to
  its edit form - the value(s) needed in the html
  editor.

  Type ref gets special treatment:
  {:selected selected :all all-selectors}

  Entries of field-key :g/entity-id
  are reversed and cut - in order to see them.
  They need to be read-only!"
  [val
   {:keys [field-type
           field-key
           disabled
           ref-path
           ref-id
           ref-selectors]
    :as   field-descriptor}]
  (cond
    (= field-type "ref")
    (let [_all-ref-data (rf/subscribe [:sub/data-path ref-path])
          to-sel-str    (fn [ref-data] (clojure.string/join
                                         " "
                                         (mapv #(% ref-data) ref-selectors)))
          all-selectors (->> @_all-ref-data
                             vals
                             (mapv #(vector (to-sel-str %) (ref-id %)))
                             (into {}))
          selected      (to-sel-str (@_all-ref-data val))]
      {:selected selected :all all-selectors})
    (= field-key :g/entity-id)
    (do
      (assert disabled "fields that are an id need to be read-only: :g/entity-id")
      (clojure.string/reverse (str (subs (clojure.string/reverse (str val)) 0 4) "...")))

    :else val))

(rf/reg-event-db
  :set/data-path
  (fn [db [_ path data]]
    (assoc-in db path data)))

(rf/reg-event-db
  :call/update-fn
  (fn [db [_ update-fn delta-to-merge]]
    (let [before (:model db)
          after  (assoc db :model (update-fn delta-to-merge))]
      after)))

(defn edit-one-field
  [label?
   {:keys [entity-map-path
           entity-id-key
           entity-id] :as entity}
   {:keys [field-key
           field-type
           disabled
           ref-path
           ref-id
           ref-selectors
           update-fn] :as field-descriptor}]
  (let [id-this-editor  (bc/next-local-id)
        conj-if-not-nil (fn [coll x]
                          (if x
                            (conj coll x)
                            coll))
        full-path       (-> entity-map-path
                            (conj-if-not-nil entity-id)
                            (conj field-key))
        _value          (rf/subscribe [:sub/data-path full-path])]
    (if ref-path
      (fn [label?
           {:keys [entity-map-path
                   entity-id-key
                   entity-id] :as entity}
           {:keys [field-key
                   field-type
                   disabled
                   ref-path
                   ref-id
                   ref-selectors
                   update-fn] :as field-descriptor}]
        (let [values      (val->edit-form @_value field-descriptor)
              selected    (:selected values)
              all         (keys (:all values))
              all-but-sel (->> all
                               (remove #{selected}))
              options     (mapv (fn [selector]
                                  (with-meta [:option selector]
                                             {:key (str id-this-editor selector)}))
                                all-but-sel)
              select      [:div.select.is-small.is-fullwidth
                           {:class @_value}
                           (into [:select
                                  {;:value     selected
                                   :class     (when disabled "disable")
                                   :on-change #(let [val (-> % .-target .-value)]
                                                 ;val-t (transform-back val field-descriptor)]
                                                 ;(println "val:" val "back:" (:all values))
                                                 (if update-fn
                                                   (let [full-event (conj update-fn {entity-id-key entity-id
                                                                                     field-key     ((:all values) val)})]
                                                     (rf/dispatch full-event))
                                                   (rf/dispatch [:set/data-path full-path ((:all values) val)])))}
                                  (with-meta [:option selected]
                                             {:key (str id-this-editor selected)})]
                                 options)]]
          (if label?
            [:div.columns.is-mobile.is-gapless.mt-0.mb-0.pt-0.pb-0
             [:div.column.is-2
              [:div.field.mt-0.mb-0.pt-0.pb-0
               [:a.button.justify-content-end.is-fullwidth.is-small.is-uppercase.has-text-weight-light.is-ghost
                (or (:field-show-as field-descriptor)
                    (:field-key field-descriptor))]]]
             [:div.column.is-4
              select]]
            select)))
      (fn [label?
           {:keys [entity-map-path
                   entity-id-key
                   entity-id] :as entity}
           {:keys [field-key
                   field-type
                   disabled
                   ref-path
                   ref-id
                   ref-selectors
                   update-fn] :as field-descriptor}]
        (let [input [:input.input.is-small.is-fullwidth
                     {:type        (or field-type "text")
                      :value       (val->edit-form @_value field-descriptor)
                      :placeholder "Text input"
                      :class       (when disabled "disable")
                      :on-change   #(let [val   (-> % .-target .-value)
                                          val-t (edit-form->val val field-descriptor)]
                                      (if update-fn
                                        (let [full-event (conj update-fn {entity-id-key entity-id
                                                                          field-key     val-t})]
                                          (rf/dispatch full-event))
                                        (rf/dispatch [:set/data-path full-path val-t])))}]]
          (if label?
            [:div.columns.is-mobile.is-gapless.mt-0.mb-0.pt-0.pb-0
             [:div.column.is-2
              [:div.field.mt-0.mb-0.pt-0.pb-0
               [:a.button.justify-content-end.is-fullwidth.is-small.is-uppercase.has-text-weight-light.is-ghost
                (or (:field-show-as field-descriptor)
                    (:field-key field-descriptor))]]]
             [:div.column.is-4
              input]]

            #_[:p.help "Do not enter the first zero"]
            #_[:div.field.is-horizontal.is-mobile
               [:div.field-label.is-small.is-fullwidth
                [:label.label.is-uppercase.has-text-left.has-text-weight-light
                 (or (:field-show-as field-descriptor)
                     (:field-key field-descriptor))]]
               [:div.field-body
                [:div.field
                 [:div.control
                  input]]]]
            input))))))

(defn edit-one-entity
  [label? {:keys [entity-id entity-map-path entity-id-key] :as entity}
   field-descriptors]
  (into [:div]
        (mapv (fn [field-descriptor]
                ;[:div.column.is-narrow.mt-0.mb-0.pt-0.pb-0
                ;^{:key (:field field-descriptor)}
                [edit-one-field label? entity field-descriptor])
              field-descriptors)))

#_(defn edit-one-entity

    [label?
     {:keys [entity-id entity-map-path entity-id-key] :as entity}
     field-descriptors]
    (into [:div] ;.columns.is-gapless.mt-0.mb-0.pt-0.pb-0]
          (edit-one-entity label? entity field-descriptors)))

(defn one-column [heading?
                  {:keys [entity-map-path
                          entity-id-key] :as entities}
                  {:keys [field-key
                          field-type
                          ref-path
                          ref-id
                          ref-selectors
                          update-fn] :as field-descriptor}]

  (let [_entity-map (rf/subscribe [:sub/data-path entity-map-path])]
    (fn [heading?
         {:keys [entity-map-path
                 entity-id-key] :as entities}
         {:keys [field-key
                 field-type
                 ref-path
                 ref-id
                 ref-selectors
                 update-fn] :as field-descriptor}]
      (let [all-entity-keys (keys @_entity-map)
            all-col-fields  (mapv (fn [entity-id]
                                    [edit-one-field
                                     false
                                     (merge entities {:entity-id entity-id})
                                     field-descriptor])
                                  all-entity-keys)]
        ;heading         (:field-show-as field-descriptor)]
        (into [:div.column.is-1b
               (when heading?
                 [:div.field.mt-0.mb-0.pt-0.pb-0
                  [:a.button.justify-content-start.is-fullwidth.is-small.is-uppercase.has-text-weight-light.is-ghost
                   (or (:field-show-as field-descriptor)
                       (:field-key field-descriptor))]]
                 #_[:label.label.is-small.is-uppercase.has-text-left.has-text-weight-light (or heading "---")])] ;.mt-0.mb-0.pt-0.pb-0]
              all-col-fields)))))


(defn edit-all-entities [heading? {:keys [entity-map-path entity-id-key] :as entities}
                         field-descriptors]
  (let [_entity-map (rf/subscribe [:sub/data-path entity-map-path])]
    (fn [heading? {:keys [entity-map-path entity-id-key] :as entities}
         field-descriptors]
      (let [
            all-cols (mapv (fn [fd]
                             [one-column heading? entities fd])
                           field-descriptors)

            #_all-entities #_(map (fn [entity-id]
                                    [edit-one-entity
                                     (merge entities {:entity-id entity-id})
                                     field-descriptors])
                                  all-entity-keys)]
        ;headings        (entity-headings field-descriptors)]
        (-> [:div.columns.is-gapless.is-mobile] ;.mt-0.mb-0.pt-0.pb-0]
            (into all-cols)
            #_(into [[:div.column.is-narrow
                      [:div "info"]
                      [:div "info2"]]]))))))


(comment
  (def test-list '(3 6 2))
  `(identity ~@test-list)
  (map #(vector %1 %2) [4 5 3] '(:a :n))
  (t/date "2003-04-23")
  #time/date"2003-04-23")


#_(defn edit-entity-field
    [{:keys [type path trigger]}]
    (let [_data (rf/subscribe [:sub/data-path path])]
      (fn []
        (println "data: " @_data ", type: " (clojure.core/type @_data) ", path: " path)
        [:input.input.is-small {:type        (or type "text")
                                :value       (val->edit-form @_data type)
                                :placeholder "Text input"
                                :on-change   #(let [val   (-> % .-target .-value)
                                                    val-t (edit-form->val val type)])}])))
;; update-fn
;; world-approach
;; (defn the-update-fn [model entity-path map-to-merge]
#_(defn edit-entity
    "An entity is a map with keys and vals that make up that entity.
  The map is identified by an :id key (that may have whatever name),
  which identifies the entity. It is called entity-id-key.
  All fields are described by a field-descriptor, that contains
  at least the :field-key and the :field-type.
  Entities are contained at a path in a data structure called model.
  E.g. the entity
       {:id 27
        :name \"Benno\"
        :num-of-bikes 3
        :wife 17
        :name-of-children [Kurt Felix Paola]
        :birth (t/date \"2000-02-15\")}
  That entity may be contained in model
  {:model-name \"Families\"
   :persons {27 {:id 27
                 :name ...}}}
  The entity-map-path is [:persons] - all entities
  The entity-path is [:persons 27] - one entity
  An entity-field-path would be [:persons 27 :name] - one field of one entity

  In order to edit a field or a complete entity,
  there are field-descriptors, that describe how to
  read, edit and write the data of the field.
  There are different kinds of fields:
  1. single values: string, number, date, etc.
  2. refs: ids that refer to other entities.
  3. collections of
    3.a refs
    3.b single values
  field-descriptors look like this:
  {:field-key   :birthday ; key of the field
   ??? needed? :id? true ; is this the id of
   :field-type        :date ;or string, number, selection, ref, boolean
   :nil-value :nothing ; default: \"no value\"; will be shown, if value is nil or invalid. May be used to set value to nil.
   :choices     [:a :b ...] ; the choices for selection
   :ref-path+selectors [ref-map-path ref-id-key [:selector-key-1-of-ref :selector-key-2-of-ref ...]
   :cardinality :many ; default :one
   :help-short \"short help text\" ; default: nil
   :help-long \"long help text\" ; default; nil
   :editor (fn[value]...) ; default: nil; a function used as editor gets val delivers new val
   :validators [(fn[value]...) \"error message 1\"
                (fn[value]...) \"error message 2\"] ; default: nil
  }
  "
    [m entity-path field-descriptors]
    (into [:div.columns]
          (mapv (fn [data]
                  [:div.column
                   ^{:key (:path data)}
                   [edit-entity-field entity-path data]])
                field-descriptors)))


#_[:div.columns
   [:div.column "First column"]
   [:div.column "Second column"]
   [:div.column "Third column"]
   [:div.column "Fourth column"]]

(comment
  ;(bc/pprint (edit-one-entity false :p1 :p2 :p3))

  (def line-descriptor {:entity-path [:g/projects "p1" :g/tasks 123]
                        :fields      [{:key        :g/start ; key of the field
                                       :type       :date ;or string, number, selection, ref, boolean
                                       :nilable    false
                                       ;:nil-value          :nothing ; default: "no value"; will be shown, if value is nil or invalid. May be used to set value to nil.
                                       ;:choices            [:a :b ...] ; the choices for selection
                                       ;:ref-path+selectors [[:g/resources] :g/entity-id [:g/entity-id :g/name]]
                                       ;:cardinality        :many ; default :one
                                       :help-short "start date of task" ; default: nil
                                       ;:help-long  "long help text" ; default; nil
                                       ;:editor     (fn [value] ...) ; default: nil; a function used as editor gets val delivers new val
                                       #_:validators #_[(fn [value] ...) "error message 1"
                                                        (fn [value] ...) "error message 2"]} ; default: nil

                                      {:key        :g/end ; key of the field
                                       :type       :date ;or string, number, selection, ref, boolean
                                       :nilable    false
                                       ;:nil-value          :nothing ; default: "no value"; will be shown, if value is nil or invalid. May be used to set value to nil.
                                       ;:choices            [:a :b ...] ; the choices for selection
                                       ;:ref-path+selectors [[:g/resources] :g/entity-id [:g/entity-id :g/name]]
                                       ;:cardinality        :many ; default :one
                                       :help-short "end date of task" ; default: nil
                                       ;:help-long  "long help text" ; default; nil
                                       ;:editor     (fn [value] ...) ; default: nil; a function used as editor gets val delivers new val
                                       #_:validators #_[(fn [value] ...) "error message 1"
                                                        (fn [value] ...) "error message 2"]}

                                      {:key        :g/capacity-needed ; key of the field
                                       :type       :number ;or string, number, selection, ref, boolean
                                       :nilable    false
                                       ;:nil-value          :nothing ; default: "no value"; will be shown, if value is nil or invalid. May be used to set value to nil.
                                       ;:choices            [:a :b ...] ; the choices for selection
                                       ;:ref-path+selectors [[:g/resources] :g/entity-id [:g/entity-id :g/name]]
                                       ;:cardinality        :many ; default :one
                                       :help-short "capacity needed for this task" ; default: nil
                                       ;:help-long  "long help text" ; default; nil
                                       ;:editor     (fn [value] ...) ; default: nil; a function used as editor gets val delivers new val
                                       #_:validators #_[(fn [value] ...) "error message 1"
                                                        (fn [value] ...) "error message 2"]}

                                      {:key                :g/resource-id ; key of the field
                                       :type               :ref ;or string, number, selection, ref, boolean
                                       :nilable            false
                                       ;:nil-value          :nothing ; default: "no value"; will be shown, if value is nil or invalid. May be used to set value to nil.
                                       ;:choices            [:a :b ...] ; the choices for selection
                                       :ref-path+selectors [[:g/resources] :g/entity-id [:g/entity-id :g/name]]
                                       ;:cardinality        :many ; default :one
                                       :help-short         "resource needed for this task task" ; default: nil
                                       ;:help-long  "long help text" ; default; nil
                                       ;:editor     (fn [value] ...) ; default: nil; a function used as editor gets val delivers new val
                                       #_:validators #_[(fn [value] ...) "error message 1"
                                                        (fn [value] ...) "error message 2"]}]})) ; default: nil]))
