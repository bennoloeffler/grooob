(ns grooob.raw-data-view.ui
  (:require
    [belib.malli :as bm]
    [re-frame.core :as rf]
    [reagent.core :as reagent]
    [reagent.dom :as rd]
    [tick.core :as t]
    [java.time :refer [LocalDate]]
    [goog.events.KeyCodes :as keycodes]
    [goog.events :as events]
    [goog.object :as gobj]
    [goog.functions]
    [clojure.pprint :refer [pprint]]
    [belib.hiccup :as bh]
    [belib.spec :as bs]
    [grooob.grid-view.ui :as grid-view]
    [grooob.model.model-spec :as ms]
    [grooob.model.model-malli :as mm]
    [grooob.utils :as utils]
    [ajax.core :as ajax]
    [goog.history.EventType :as HistoryEventType]
    [time-literals.read-write]
    [luminus-transit.time :as time]
    [cognitect.transit :as transit]
    [re-pressed.core :as rp]
    [grooob.projects-overview.events :as e]
    [grooob.comps.ui :as cui]
    [grooob.raw-data-view.events]))

(comment
  (bm/hum-err [:vector :grooob-resource] [{:id   3,
                                           :name "r3",
                                           :capa {(t/date "2024-11-30") {:yellow 20, :red 30}}}]))

(defn raw-data-form-old []
  (let [_dirty         (reagent/atom false)
        _data-buffer   (reagent/atom (with-out-str (pprint (:resources mm/test-model))))
        _error         (reagent/atom nil)
        try-parse-data (fn []
                         (let [data (try (clojure.edn/read-string @_data-buffer)
                                         (catch :default e {:data nil :error (ex-message e)}))
                               err  (or (:error data)
                                        (bm/hum-err [:vector :grooob-resource] data))]
                           (if err {:data nil :error err}
                                   {:data data :error nil})))

        save-resources (fn []
                         (println "save resources, parsed data:")
                         (let [data (clojure.edn/read-string @_data-buffer)
                               err  (bm/hum-err [:vector :grooob-resource] data)]
                           (if err (println err)
                                   (do
                                     (reset! _dirty false)
                                     (println data)))))]

    (fn []
      [:<>
       [cui/overview-proj-details-menu]
       ;[:div "project-raw-data" #_(str (last (get (vec (:projects @_project)) (:y @cross))))]
       #_[:pre (with-out-str (pprint @_model))]

       #_[:button.button.is-primary "save"]
       [:a.button.is-primary.is-outlined
        {:on-click save-resources
         :disabled (or @_error (not @_dirty))}
        [:span.icon.is-large>i.fas.fa-1x (bh/cs (if @_dirty "fa-cloud-arrow-up" "fa-check") #_fa-floppy-disk)]
        [:span [:b "save RESOURCES"]]]
       ;[:pre (str "error = " (or @_error "nil") ", dirty = " @_dirty ", disabled = (or @_error (not @_dirty)) = " (or @_error (not @_dirty)))]
       ; TODO remove load, transform
       [:textarea.textarea {:placeholder "no errors..."
                            :width       "100%"
                            :heigth      "100%"
                            :rows        "2"
                            :read-only   true
                            :value       (or @_error "")}]

       [:textarea.textarea {:placeholder "the raw model here"
                            :width       "100%"
                            :heigth      "100%"
                            :rows        "20"
                            :value       @_data-buffer
                            :on-change   #(let [val (-> % .-target .-value)]
                                            (reset! _data-buffer val)
                                            (reset! _dirty true)
                                            (reset! _error (or (str (:error (try-parse-data)))
                                                               nil)))}]
       ;(println "SET VAL:")
       ;(println val))}]


       ;[:pre (with-out-str (pprint @project))]

       #_[projects-details-form
          component-id
          _project
          (e/project-details-view-keydown-rules component-id _project)]])))


(defn raw-data-form-2 [data #_(:resources mm/test-model)
                       schema #_[:vector :grooob-resource]
                       save-fn
                       button-str #_RESOURCES]
  (let [_state         (reagent/atom {:data-buffer (with-out-str (pprint data))
                                      :dirty       false
                                      :error       nil})
        ;_dirty         (reagent/atom false)
        ;_data-buffer   (reagent/atom (with-out-str (pprint data)))
        ;_error         (reagent/atom nil)
        try-parse-data (fn [d]
                         (let [data (try (clojure.edn/read-string d)
                                         (catch :default e {:data nil :error (ex-message e)}))
                               err  (or (:error data)
                                        (bm/hum-err schema data))]
                           (if err {:data nil :error err}
                                   {:data data :error nil})))
        save-resources (fn [d]
                         ;(println "save parsed data:")
                         (let [data (clojure.edn/read-string d)]
                           (swap! _state merge {:dirty false})
                           (save-fn data)))]
    (fn []
      [:form.box
       [:div.columns.is-mobile.is-gapless
        [:div.column.is-2
         [:a.button.is-primary.is-outlined.is-fullwidth
          {:on-click #(save-resources (:data-buffer @_state))
           :disabled (or (:error @_state) (not (:dirty @_state)))}
          [:span.icon.is-large>i.fas.fa-1x (bh/cs (if (:dirty @_state) "fa-cloud-arrow-up" "fa-check") #_fa-floppy-disk)]
          [:span [:b (if (:dirty @_state) (str "save " button-str)
                                          "saved")]]]]
        [:div.column
         [:textarea.textarea {:placeholder "no errors..."
                              :width       "100%"
                              :heigth      "100%"
                              :rows        "2"
                              :read-only   true
                              :value       (or (:error @_state) "")}]]]
       [:textarea.textarea {:placeholder "the raw data"
                            :width       "100%"
                            :heigth      "100%"
                            :rows        "20"
                            :value       (:data-buffer @_state)
                            :on-change   #(let [val (-> % .-target .-value)
                                                err (:error (try-parse-data val))
                                                err (if err (str err) nil)]
                                            (swap! _state merge {:data-buffer val
                                                                 :dirty       true
                                                                 :error       err}))}]])))
;(reset! _error (if err (str err) nil))
;(reset! _data-buffer val)
;(reset! _dirty true))}]])))



(defn raw-data-form []

  ;; TODO
  ;;  ??? 1. strip-start-end (all superflouos start / end, all -cw)
  ;;  ??? 2. instrument-all-start-end
  ;;  ok 3. make fn
  ;;  ok 4. get :projects
  ;;  ok 5. get :resources
  ;;  ok 6. write back :projects
  ;;  ok 7. write back :resources

  (let [_resources (rf/subscribe [:sub/data-path [:model :resources]])
        _projects  (rf/subscribe [:sub/data-path [:model :projects]])]
    (fn [] [:div.container>div.content
            ;[:pre (with-out-str (pprint @_resources))]
            [cui/overview-proj-details-menu]
            [raw-data-form-2
             @_resources
             [:vector :grooob-resource]
             (fn [data]
               ;(println "saving: " data)
               (rf/dispatch [:raw-data-view/save :resources data]))
             "RESOURCES"]
            [:br]
            [raw-data-form-2
             @_projects
             [:vector :grooob-project]
             (fn [data]
               ;(println "saving: " data)
               (rf/dispatch [:raw-data-view/save :projects data]))
             "PROJECTS"]])))



