(ns grooob.comps.ui
  (:require
    [cuerdas.core :as str]
    [re-frame.core :as rf]
    [reagent.core :as r]
    [reagent.ratom]
    [belib.core :as bc]
    [belib.hiccup :as bh]
    [belib.malli :as bm]
    [belib.date-parse-messy :as bpm]
    [tick.core :as t]
    [grooob.time-transit]))




(defn change-once-after
  "Returns a reagent/atom with the value: before.
  Changes it once after time to value: then.
  Example:
  (defn login-with-google-button-1 []
    (let [bounce-off (change-once-after 3000 \"fa-bounce\" nil)]
      (fn []
        [:span.icon.is-large>i
          (bh/cs @bounce-off \"fas fa-lg fa-brands fa-google\")])))"
  [time before then]
  (let [to-change (r/atom before)]
    (js/setTimeout #(reset! to-change then) time)
    to-change))

(defn change-continuously
  "returns a reagent/atom and changes it every
  intervall milliseconds to the next value
  in the collection coll."
  [start intervall coll]
  (let [idx       (atom 0)
        to-change (r/atom (get coll @idx))]
    (js/setTimeout #(letfn [(change-it []
                              (swap! idx inc)
                              (when (< @idx (count coll))
                                (reset! to-change (get coll @idx))
                                (js/setTimeout change-it intervall)))]
                      (js/setTimeout change-it intervall))
                   start)
    to-change))

(comment
  (def coll ["a" "b"])
  (get coll -1))

(defn fa-smaller
  "Makes a fontawesome icon VERY big and then very fast smaller.
   Works together with helper from belib/hiccup: bh/cs.
   Example:
   (defn register-button []
     (let [smaller (cui/fa-smaller)]
         (fn []
           [:a.button.is-outlined.mr-1.is-fullwidth.is-primary
             {:href \"#/register\"}
             [:span.icon.is-large>i (bh/cs @smaller \"fas fa-pen-nib\")]
             [:span \"create free account\"]])))
   "
  []
  (change-continuously 1000 35 ["fa-lg" "fa-10x" "fa-9x" "fa-8x" "fa-7x" "fa-6x" "fa-5x" "fa-4x" "fa-3x" "fa-2x" "fa-1x" "fa-sm" "fa-xs" "fa-2xs" "fa-2xs" "fa-xs" "fa-sm" "fa-1x" "fa-lg"]))

(defn fa-bounce-off
  "Makes a fontawesome icon bouncing for some seconds and then stop.
   Works together with helper from belib/hiccup: bh/cs.
   Example:
   (defn login-with-google-button []
     (let [bounce-off (cui/fa-bounce-off)]
       (fn []
         [:a.button.is-outlined.mr-1.is-fullwidth
           {:href \"/login-with-google\"}
           [:span.icon.is-large>i (bh/cs @bounce-off \"fas fa-lg fa-brands fa-google\")]
           [:span \"login with google account\"]])))
  "
  []
  (change-once-after 3000 "fa-bounce" nil))

(comment
  (def ratom (r/atom {}))
  (type ratom))

; https://dhruvp.github.io/2015/02/23/mailchimip-clojure/
(defn input-field-old
  "Assoc data-atom key value-of-input at every key stroke."
  [data-atom key type placeholder icon-left validator-fn validate-at-start]
  (let [local-data       (r/atom (key @data-atom))
        error            (r/atom nil)
        validate         (r/atom validate-at-start)
        validate-fn      (or validator-fn #(identity nil))
        call-validate-fn (fn [] (let [new-error (validate-fn (key @local-data))]
                                  (when (not= new-error @error)
                                    (reset! error new-error))))]
    (fn [data-atom key type placeholder icon-left validator-fn validate-at-start]
      (let [icon-left (if (vector? icon-left)
                        (str/join " " (->> (filter identity icon-left)
                                           (map #(if (instance? reagent.ratom/RAtom %)
                                                   (deref %)
                                                   %))))
                        icon-left)
            try-val   #(when @validate
                         (reset! error (validate-fn @local-data)))
            try-set   #(do (println "try-set...")
                           (reset! validate true)
                           (try-val)
                           (when-not @error
                             (swap! data-atom assoc key @local-data)
                             (println "data was set:" @data-atom)))]
        (call-validate-fn) ; always! in case another data field changed
        [:div.field
         [:p.control.has-icons-left.has-icons-right
          [:input.input
           {:value       @local-data
            :type        (name type)
            :placeholder placeholder
            :on-change   #(let [val (-> % .-target .-value)]
                            (println "val:" val "key:" key "local-data:" @local-data)
                            (reset! local-data val)
                            (try-val)
                            (println "local data:" @local-data)
                            (println "error:" @error)
                            (println "validate:" @validate))
            :on-blur     try-set
            :on-key-up   (fn [event]
                           (when (= (.-keyCode event) 13)
                             (try-set)))}]

          ;(println (str val)))}]
          [:span.icon.is-small.is-left
           [:i (bh/cs "fas" icon-left #_fa-envelope)]]
          [:span.icon.is-small.is-right {:style {:color grooob.utils/primary-color}}
           [:i (bh/cs "fas" (if @validate (if @error "fa-exclamation" "fa-check") ""))]]
          ;(println @validate @error)
          (when (and @validate @error)
            (into [:<>] (map (fn [err] [:<> [:span (str err)] [:br]]) @error)))]]))))

(defn input-field-old-2
  "Creates an input field that validates on pressing return or leaving the field.
   Or when validate-at-start is true."
  [data-atom key type placeholder icon-left user-validate-fn validating-from-start]
  (let [local-data     (r/atom (get @data-atom key))
        error          (r/atom nil)
        validating     (r/atom validating-from-start)
        validate-fn    (or user-validate-fn #(identity nil))
        check-error-fn (fn [] (let [new-error (validate-fn @local-data)]
                                (when (not= new-error @error)
                                  (reset! error new-error))))]

    (fn [data-atom key type placeholder icon-left user-validate-fn validating-from-start]
      (let [validate-fn   (or user-validate-fn #(identity nil))
            update-local  (fn [val]
                            (reset! local-data (str val))
                            (when @validating
                              (reset! error (validate-fn @local-data))))
            update-global (fn []
                            (when-not @error
                              (swap! data-atom assoc key @local-data)))
            validate-on   (fn [] (reset! validating true))
            validate      (fn []
                            (reset! error (validate-fn @local-data))
                            (when-not @error
                              (swap! data-atom assoc key @local-data)))
            on-key-up     (fn [event]
                            (when (= 13 (.-keyCode event)) ; keyCode for return
                              (validate)
                              (update-global)))]
        (check-error-fn)
        [:div.field
         [:p.control.has-icons-left.has-icons-right
          [:input.input {:type        (name type)
                         :placeholder placeholder
                         :value       @local-data
                         :on-change   #(update-local (-> % .-target .-value))
                         :on-blur     #(do (validate-on) (validate))
                         :on-key-up   on-key-up}]
          (when icon-left
            [:span.icon.is-left [:i {:class (str "fas " icon-left)}]])
          (when @validating
            [:span.icon.is-right [:i {:class (str "fas " (if @error "fa-exclamation" "fa-check"))}]])
          (when (and @validating @error)
            [:<> (for [err @error]
                   ^{:key err} [:p err])])]]))))

(defn errors-in-date [date-str]
  ;(println "val: " date-str)
  (if date-str
    (try (t/date date-str)
         nil ; no error
         (catch js/Error e ["Provide a valid date."]))
    ["Provide a valid date."]))

(comment
  (errors-in-date "2021-1-01") ; => ["Provide valid date."]
  (errors-in-date "2021-11-01")) ; => nil

nil

(defn errors-in-email [val] (bm/hum-err bm/email-schema val))


(defn input-field
  "Creates an input field that validates on pressing return or leaving the field.
   Or when validate-at-start is true."
  [data-atom key type placeholder icon-left user-validate-fn validating-from-start write-through]
  (let [dirty          (r/atom false)
        local-data     (r/atom (get @data-atom key))
        error          (r/atom nil)
        validating     (r/atom validating-from-start)
        validate-fn    (or user-validate-fn
                           (when (= type "date") errors-in-date)
                           (when (= type "email") errors-in-email)
                           #(identity nil))
        check-error-fn (fn [] (let [new-error (validate-fn @local-data)]
                                (when (not= new-error @error)
                                  (reset! error new-error))))
        err-style      {:style {:color grooob.utils/primary-color}}]

    (fn [data-atom key type placeholder icon-left user-validate-fn validating-from-start write-through]
      (let [;; in order to let the icon use "changing css", like
            icon-left     (if (vector? icon-left)
                            (str/join " " (->> (filter identity icon-left)
                                               (map #(if (instance? reagent.ratom/RAtom %)
                                                       (deref %)
                                                       %))))
                            icon-left)

            validate      (fn []
                            (when @validating
                              (reset! error (validate-fn @local-data))))

            update-local  (fn [val]
                            (reset! local-data (str val))
                            (reset! dirty true)
                            (validate)
                            ;(println "write through: " write-through)
                            (when write-through
                              ;(println "pw write through" key @local-data)
                              (reset! dirty false)
                              (swap! data-atom assoc key @local-data)))

            update-global (fn []
                            (reset! validating true)
                            (validate)
                            (when-not @error
                              (reset! dirty false)
                              (swap! data-atom assoc key @local-data)))

            on-key-up     (fn [event]
                            (when (= 13 (.-keyCode event)) ; keyCode for return
                              (update-global)))]
        (check-error-fn)
        [:div
         [:div.field
          [:p.control.has-icons-left.has-icons-right
           [:input.input {:type        type
                          :placeholder placeholder
                          :value       @local-data
                          :on-change   #(update-local (-> % .-target .-value))
                          :on-blur     update-global
                          :on-key-up   on-key-up}]
           (when icon-left
             [:span.icon.is-left
              [:i {:class (str "fas " icon-left)}]])
           (when @validating
             [:span.icon.is-right
              [:i {:style {:color (if @dirty grooob.utils/primary-color "green")}
                   :class (str "fas " (if @error "fa-exclamation" "fa-check"))}]])]]
         (when (and @validating @error)
           (into [:<>] (for [err @error]
                         ^{:key err}
                         [:p err-style err])))]))))


(defn errors-in-password [val] (bm/hum-err bm/password-schema val))


(defn input-password-field
  "Assoc data-atom key value-of-input at every key stroke."
  [data-atom key placeholder icon-left validator-fn write-through]
  ;(println "SETUP" placeholder validator-fn)
  (let [dirty            (r/atom false)
        local-data       (r/atom (get @data-atom key))
        error            (r/atom nil)
        hide-pw          (r/atom true)
        validate         (r/atom false)
        i-left           (or icon-left "fa-lock")
        validate-fn      (or validator-fn errors-in-password)
        call-validate-fn (fn [] (let [new-err (validate-fn @local-data)]
                                  (when (not= new-err @error)
                                    (reset! error (validate-fn @local-data)))))
        update-global    (fn []
                           (reset! validate true)
                           (call-validate-fn)
                           (when-not @error
                             (reset! dirty false)
                             (swap! data-atom assoc key @local-data))
                           (when write-through
                             ;(println "pw write through" key @local-data)
                             (reset! dirty false)
                             (swap! data-atom assoc key @local-data)))
        on-key-up        (fn [event]
                           (when (= 13 (.-keyCode event)) ; keyCode for return
                             (update-global)))]
    (fn [data-atom key placeholder icon-left validator-fn write-through]
      (let [validate-fn (or validator-fn errors-in-password)]
        ;(println "DRAW: " placeholder validator-fn)
        (call-validate-fn) ; always! in case another data field changed
        [:div.field
         ;;
         ;[:div (do (call-validate-fn) nil)]
         [:p.control.has-icons-left.has-icons-right
          [:input.input
           {:value       @local-data
            :type        (if @hide-pw "password" "text")
            :placeholder placeholder
            :on-change   (fn [e] (let [val (-> e .-target .-value)]
                                   (reset! local-data val)
                                   (reset! dirty true)
                                   (call-validate-fn)
                                   (when write-through
                                     ;(println "pw write through" key @local-data)
                                     (reset! dirty false)
                                     (swap! data-atom assoc key @local-data))))

            ;(println @data-atom)))
            :on-blur     update-global
            :on-key-up   on-key-up}]
          ;(println (str val)))}]
          [:span.icon.is-small.is-left
           [:i (bh/cs "fas" i-left) #_fa-envelope]]
          [:span.icon.fav-icon.is-small.is-right
           {:style    {:color grooob.utils/primary-color}
            :on-click #(swap! hide-pw not)}
           [:i.fav-icon (merge (bh/cs "fas" (if @hide-pw "fa-eye" "fa-eye-slash" #_"fa-exclamation" #_"fa-check"))
                               {:style {:color (if @validate
                                                 (if @dirty grooob.utils/primary-color "green")
                                                 "black")}})]]
          (when (and @validate @error)
            (into [:<>]
                  (map (fn [err]
                         [:<> [:span {:style {:color grooob.utils/primary-color}}
                               (str err)] [:br]]) @error)))]]))))


(defn overview-proj-details-menu []
  (let [_page (rf/subscribe [:common/page-id])]
    (fn []
      ;(println "page: " @_page)
      [:div.tabs ;.is-small ;.is-centered.is-boxed
       [:ul
        [:li {:class (when (= @_page :models-list) :is-active)} #_(bh/cs (when (= @_page :projects-portfolio) :is-active))
         [:a {:on-click #(rf/dispatch [:common/navigate! :models-list nil nil])} "Models"
          [:i.icon.fas.fa-house]]]
        [:li {:class (when (= @_page :projects-portfolio) :is-active)} #_(bh/cs (when (= @_page :projects-portfolio) :is-active))
         [:a {:on-click #(rf/dispatch [:common/navigate! :projects-portfolio nil nil])} "All"
          [:i.icon.fas.fa-bars-staggered #_fa-table-cells]]]
        [:li {:class (when (= @_page :project) :is-active)}
         [:a {:on-click #(rf/dispatch [:common/navigate! :project nil nil])} "One"
          [:i.icon.fas.fa-chart-gantt]]]
        [:li {:class (when (= @_page :project-details) :is-active)}
         [:a {:on-click #(rf/dispatch [:common/navigate! :project-details nil nil])} "Details"
          [:i.icon.fas.fa-table-cells]]]
        [:li {:class (when (= @_page :raw-data) :is-active)}
         [:a {:on-click #(rf/dispatch [:common/navigate! :raw-data nil nil])} "Raw"
          [:i.icon.fas.fa-file-lines]]]]])))

;;
;; ---------------- UI components for re-frame entities -----------------------------
;;


(defn edit-val->data-val
  "transforms the value in its edit form (mostly string)
  in the html browser editor back to the value
  in its data form."
  [edit-val
   {:keys [field-type]
    :as   field-descriptor}]
  (case field-type
    "number" (long edit-val)
    "date" (t/date edit-val)
    "ref" (ex-info "don't use for refs" {:val              edit-val
                                         :field-descriptor field-descriptor})
    edit-val))


#_(defn edit-form->val
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



(defn data-val->edit-val
  "transforms a value from the data model to
  its edit form - the value(s) needed in the html
  editor.

  Type ref gets special treatment:
  {:selected selected :all all-selectors}

  Entries of field-key :id
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
          all-ref-data  (if (map? @_all-ref-data) @_all-ref-data (bc/id-map @_all-ref-data ref-id))
          to-sel-str    (fn [ref-data] (clojure.string/join
                                         " "
                                         (mapv #(% ref-data) ref-selectors)))
          all-selectors (->> all-ref-data
                             vals
                             (mapv #(vector (to-sel-str %) (ref-id %)))
                             (into {}))
          selected      (to-sel-str (all-ref-data val))]
      {:selected selected :all all-selectors})

    (= field-key :id)
    (do
      (assert disabled "fields that are an id need to be read-only: :id {... :disabled  true}")
      (clojure.string/reverse (str (subs (clojure.string/reverse (str val)) 0 4) "...")))
    :else val))


#_(defn val->edit-form
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
            all-ref-data  (if (map? @_all-ref-data) @_all-ref-data (id-map @_all-ref-data ref-id))
            to-sel-str    (fn [ref-data] (clojure.string/join
                                           " "
                                           (mapv #(% ref-data) ref-selectors)))
            all-selectors (->> all-ref-data
                               vals
                               (mapv #(vector (to-sel-str %) (ref-id %)))
                               (into {}))
            selected      (to-sel-str (all-ref-data val))]
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
  [{:keys [entities-path
           entity-id] :as entity}
   {:keys [field-key
           field-type
           disabled
           ref-path
           ref-id
           ref-selectors
           update-fn] :as field-descriptor}
   {:keys [label?] :as visual}]
  (let [id-this-editor (bc/next-local-id)
        full-path      (-> entities-path
                           (conj entity-id)
                           (conj field-key))
        _value         (rf/subscribe [:sub/data-path full-path])
        orig-value     @_value
        _              (when-not @_value (throw (ex-info (str "no value for field-descriptor for: " field-key)
                                                         {:full-path        full-path
                                                          :field-descriptor field-descriptor
                                                          :value            @_value})))]
    (if ref-path
      (fn [{:keys [entity-map-path
                   entity-id-key
                   entity-id] :as entity}
           {:keys [field-key
                   field-type
                   disabled
                   ref-path
                   ref-id
                   ref-selectors
                   update-fn] :as field-descriptor}
           {:keys [label?] :as visual}]
        (let [values      (data-val->edit-val @_value field-descriptor)
              selected    (:selected values)
              all         (keys (:all values))
              all-but-sel (->> all
                               (remove #{selected}))
              options     (mapv (fn [selector]
                                  (with-meta [:option selector]
                                             {:key (str selector)}))
                                all-but-sel)
              select      [:div.select.is-fullwidth
                           ;{:class @_value}
                           (into [:select
                                  {;:value     selected
                                   :class     (when disabled "disable")
                                   :on-change #(let [val (-> % .-target .-value)]
                                                 ;val-t (transform-back val field-descriptor)]
                                                 ;(println "val:" val "back:" (:all values))
                                                 (if update-fn
                                                   (update-fn {:full-path full-path
                                                               :value     ((:all values) val)})
                                                   (rf/dispatch [:set/data-path full-path ((:all values) val)])))}
                                  (with-meta [:option selected]
                                             {:key (str selected id-this-editor)})]
                                 options)]]
          (if label?
            [:div.columns.is-mobile.is-gapless.mt-0.mb-0.pt-0.pb-0
             [:div.column.is-2
              [:div.field.mt-0.mb-0.pt-0.pb-0
               [:a.button.justify-content-end.is-fullwidth.is-uppercase.has-text-weight-light.is-ghost
                (or (:field-show-as field-descriptor)
                    (:field-key field-descriptor))]]]
             [:div.column.is-4
              select]]
            select)))
      (fn [{:keys [entities-path
                   entity-id] :as entity}
           {:keys [field-key
                   field-type
                   disabled
                   update-fn] :as field-descriptor}
           {:keys [label?] :as visual}]
        (let [;value        @_value
              _local-value (r/atom (data-val->edit-val orig-value field-descriptor))
              _            (when-not @_local-value (throw (ex-info (str "no value for field-descriptor: " field-key)
                                                                   {:full-path        full-path
                                                                    :field-descriptor field-descriptor})))
              field-type   (if (= field-type "date") "text" field-type)
              input        [:input.input.is-fullwidth
                            {:type        (or field-type "text")
                             :value       @_local-value
                             :placeholder "Text input"
                             :class       (when disabled "disable")
                             :on-blur     #(let [val   @_local-value
                                                 val-t (edit-val->data-val val field-descriptor)]
                                             (if update-fn
                                               (update-fn {:full-path full-path
                                                           :value     val-t})
                                               (rf/dispatch [:set/data-path full-path val-t])))
                             :on-change   #(let [val (-> % .-target .-value)]
                                             (reset! _local-value val))}]]
          (if label?
            [:div.columns.is-mobile.is-gapless.mt-0.mb-0.pt-0.pb-0
             [:div.column.is-2
              [:div.field.mt-0.mb-0.pt-0.pb-0
               [:a.button.justify-content-end.is-fullwidth.is-uppercase.has-text-weight-light.is-ghost
                (or (:field-show-as field-descriptor)
                    (:field-key field-descriptor))]]]
             [:div.column.is-4
              input]]
            input))))))

#_(defn edit-one-field
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
          (let [value @_value
                _     (when-not value (throw (ex-info (str "no value for field-descriptor: " field-key)
                                                      {:full-path        full-path
                                                       :field-descriptor field-descriptor})))
                input [:input.input.is-small.is-fullwidth
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
  [entity field-descriptors visual]
  (into [:div]
        (mapv (fn [field-descriptor]
                [edit-one-field entity field-descriptor visual])
              field-descriptors)))

#_(defn edit-one-entity
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
                  {:keys [entities-path
                          entity-id-key] :as entities}
                  {:keys [field-key
                          field-type
                          ref-path
                          ref-id
                          ref-selectors
                          update-fn] :as field-descriptor}]

  (let [_entities (rf/subscribe [:sub/data-path entities-path])
        _         (assert (not= nil @_entities) (str "the entities-path points to nil: " entities-path))]
    (fn [heading?
         {:keys [entities-path
                 entity-id-key] :as entities}
         {:keys [field-key
                 field-type
                 ref-path
                 ref-id
                 ref-selectors
                 update-fn] :as field-descriptor}]


      (let [all-entity-keys (keys (bc/idx-map @_entities))
            all-col-fields  (mapv (fn [entity-id]
                                    [edit-one-field
                                     #_false
                                     {:entities-path entities-path
                                      :entity-id     entity-id} #_entities
                                     field-descriptor])
                                  all-entity-keys)]
        ;heading         (:field-show-as field-descriptor)]
        (into [:div.column.is-1b
               (when heading?
                 [:div.field.mt-0.mb-0.pt-0.pb-0
                  [:a.button.justify-content-start.is-fullwidth.is-uppercase.has-text-weight-light.is-ghost
                   (or (:field-show-as field-descriptor)
                       (:field-key field-descriptor))]]
                 #_[:label.label.is-small.is-uppercase.has-text-left.has-text-weight-light (or heading "---")])] ;.mt-0.mb-0.pt-0.pb-0]
              all-col-fields)))))

#_(defn one-column [heading?
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

(defn param-edit-all-entities [{{:keys [entities-path entity-id-key] :as entities} :entities
                                field-descriptors                                  :field-descriptors
                                {:keys [heading?] :as visuals}                     :visuals}]

  (println "entities: ")
  (bc/p entities)
  (println "field-descriptors: ")
  (bc/p field-descriptors)
  (println "visuals:" visuals))

(comment
  (param-edit-all-entities
    {:entities          {:entities-path [:model :projects 0 :tasks]
                         :entity-id-key :id}
     :visuals           {:heading? true}
     :field-descriptors [{:field-key :id
                          ;:field-type "number"
                          ;:field-show-as "ID"
                          :disabled  true}

                         {:field-key     :start
                          :field-type    "date"
                          :field-show-as "Start"}
                         ;:update-fn     [:update-task p-id]}

                         {:field-key     :end
                          :field-type    "date"
                          :field-show-as "End"}
                         ;:update-fn     [:update-task p-id]}

                         {:field-key     :resource-id
                          :field-show-as "Resource"
                          :field-type    "ref"
                          ;:disabled      true
                          :ref-path      [:model :resources]
                          :ref-id        :id
                          :ref-selectors [:name :id]}
                         ;:update-fn     [:update-task p-id]}

                         {:field-key     :capa-need
                          :field-type    "number"
                          :field-show-as "Capacity"
                          :update-fn     (fn [path delta] (println path, delta))}

                         {:field-key :name
                          #_:update-fn  #_[:update-task p-id]}]}))

(defn edit-all-entities [heading? {:keys [entities-path entity-id-key] :as entities}
                         field-descriptors]
  ;(let [_entity-map (rf/subscribe [:sub/data-path entities-path])]
  ;(fn [heading? {:keys [entities-path entity-id-key] :as entities}
  ;     field-descriptors
  (let [all-cols (mapv (fn [fd]
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
                  [:div "info2"]]]))))

#_(defn edit-all-entities [heading? {:keys [entity-map-path entity-id-key] :as entities}
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

(def data-vec [{:id    1
                :name  "abc"
                :start (t/date "2003-03-04")}
               {:id    2
                :name  "abcde"
                :start (t/date "2023-09-09")}])
(def data-map {1 {:id    1
                  :name  "abc"
                  :start (t/date "2003-03-04")}
               2 {:id    2
                  :name  "abcde"
                  :start (t/date "2023-09-09")}})
(comment
  {:entity {:entities-path []
            :entity-id     1}
   :field  {:field-key :name
            :update-fn :str/upper}}

  (get-in data-vec [0 :name])
  (get-in data-map [2 :name])
  (update-in data-vec [0 :name] str/upper)
  (update-in data-map [2 :name] str/upper))