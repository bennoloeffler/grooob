(ns grooob.comps.ui-newer
  (:require
    [cuerdas.core :as str]
    [reagent.core :as r]
    [belib.malli :as bm]
    [belib.hiccup :as bh]

    [tick.core :as t]

    [hyperfiddle.rcf :refer [tests]]))

(hyperfiddle.rcf/enable! true)

;; ----------------------------------------
;; icon animation helpers
;; ----------------------------------------

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


;; ----------------------------------------
;; validation helpers
;; ----------------------------------------

(defn errors-in-password [val] (bm/hum-err bm/password-schema val))

(defn errors-in-date [date-str]
  (if date-str
    (try (t/date date-str)
         nil ; no error
         (catch js/Error e ["Provide a valid date."]))
    ["Provide a valid date."]))

(tests
  (errors-in-date "2021-1-01") := ["Provide a valid date."]
  (errors-in-date "2021-11-01") := nil
  :end-tests)

(defn errors-in-email [val] (bm/hum-err bm/email-schema val))


;; ----------------------------------------
;; password and input field
;; ----------------------------------------


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

;; TODO
;; show errors, when validator fails
;; show ! when validating and fails
;; show ok when validating and ok


(defn input-field-new-old
  [{:keys [data-atom key type placeholder icon-left validate-fn validating-from-start write-through]
    :or   {type                  "text"
           validate-fn           (or (when (= type "password") errors-in-password)
                                     (when (= type "date") errors-in-date)
                                     (when (= type "email") errors-in-email)
                                     #(identity nil))
           validating-from-start false
           write-through         false}}]
  (let [password?                  (= type "password")
        dirty?                     (r/atom false)
        local-data                 (r/atom (get @data-atom key))
        error                      (r/atom nil)
        validating?                (r/atom validating-from-start)
        input-type                 (r/atom type)
        toggle-password-visibility (fn [] (reset! input-type (if (= @input-type "password") "text" "password")))
        update-local               (fn [val]
                                     (reset! local-data val)
                                     (reset! dirty? true)
                                     (validate-fn val)
                                     (when write-through
                                       (reset! dirty? false)
                                       (swap! data-atom assoc key val)))
        update-global              (fn []
                                     (reset! validating? true)
                                     (validate-fn @local-data)
                                     (when-not @error
                                       (reset! dirty? false)
                                       (swap! data-atom assoc key @local-data)))]
    (fn []
      [:div.field
       [:p.control.has-icons-left.has-icons-right
        [:input.input
         {:type        @input-type
          :placeholder placeholder
          :value       @local-data
          :on-change   #(update-local (-> % .-target .-value))
          :on-blur     update-global}]
        (when icon-left
          [:span.icon.is-left
           [:i {:class (str "fas " icon-left)}]])
        (when password?
          [:span.icon.is-right
           {:on-click toggle-password-visibility}
           [:i {:class (str "fas " (if (= @input-type "password") "fa-eye" "fa-eye-slash"))}]])
        (when (and @validating? @error)
          (into [:<>] (for [err @error]
                        ^{:key err}
                        [:p {:style {:color "red"}} err])))]])))

;; TODO
;; if data atom is updated: view should update
(defn input-field-new
  "input field with default for:
   text, password, date, email, number.
  "
  [{:keys [data-atom key type placeholder icon-left validate-fn validating-from-start write-through]
    :or   {type                  "text"
           placeholder           nil
           icon-left             (or (when (= type "password") "fa-lock")
                                     (when (= type "date") "fa-calendar")
                                     (when (= type "email") "fa-envelope"))

           validate-fn           (or (when (= type "password") errors-in-password)
                                     (when (= type "date") errors-in-date)
                                     (when (= type "email") errors-in-email)
                                     #(identity nil))
           validating-from-start false
           write-through         false}}]
  (assert data-atom "data-atom is required! Use r/atom. or atom.")
  (assert key "key is required!")
  (let [dirty?           (r/atom false)
        hide-pw?         (r/atom true)
        local-data       (r/atom (get @data-atom key))
        error            (r/atom nil)
        validating?      (r/atom validating-from-start)
        call-validate-fn #(let [new-err (validate-fn @local-data)]
                            (when (not= new-err @error)
                              ;(println "call-validate-fn, error= " new-err)
                              (reset! error new-err)))
        update-local     (fn [val]
                           ;(println "update-local, val= " val)
                           (reset! local-data val)
                           (reset! dirty? true)
                           (when @validating?
                             (call-validate-fn))
                           (when (and write-through (not @error))
                             (reset! dirty? false)
                             (swap! data-atom assoc key val)))
        update-global    (fn []
                           (reset! validating? true)
                           (call-validate-fn)
                           ;(println "update-global, local= " @local-data)
                           (when-not @error
                             (reset! dirty? false)
                             (swap! data-atom assoc key @local-data)))
        update-global-13 (fn [event]
                           (when (= 13 (.-keyCode event)) ; keyCode for return
                             ;(println "on-key-up")
                             (update-global)))]
    (fn []
      (call-validate-fn)
      [:div.field
       [:p.control.has-icons-left.has-icons-right
        [:input.input
         {:type        (if (and (= type "password") (not @hide-pw?))
                         "text"
                         type)
          :placeholder placeholder
          :value       @local-data
          :on-change   #(update-local (-> % .-target .-value))
          :on-blur     update-global
          :on-key-up   update-global-13}]
        (when icon-left
          [:span.icon.is-left
           [:i {:class (str "fas " icon-left)}]])
        (if (= type "password")
          [:span.icon.fav-icon.is-small.is-right
           {:style    {:color grooob.utils/primary-color}
            :on-click #(swap! hide-pw? not)}
           [:i.fav-icon (merge (bh/cs "fas" (if @hide-pw? "fa-eye" "fa-eye-slash" #_"fa-exclamation" #_"fa-check"))
                               {:style {:color (if @validating?
                                                 (if (or @error @dirty?) grooob.utils/primary-color "green")
                                                 "grey")}})]]
          (when @validating?
            [:span.icon.is-right
             [:i {:style {:color (if (or @error @dirty?) grooob.utils/primary-color "green")}
                  :class (str "fas " (if @error "fa-exclamation" "fa-check"))}]]))]
       (when (and @validating? @error)
         (into [:<>] (for [err @error]
                       ^{:key err}
                       [:p {:style {:color grooob.utils/primary-color}} err])))])))



