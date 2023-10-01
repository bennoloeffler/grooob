(ns grooob.project-details.ui
  (:require [re-frame.core :as rf]
            [grooob.model.model :as model]
            [reagent.core :as reagent]
            [reagent.dom :as rd]
            [tick.core :as t]
            [java.time :refer [LocalDate]]
            [goog.events.KeyCodes :as keycodes]
            [goog.events :as events]
            [goog.object :as gobj]
            [goog.functions]
            [clojure.pprint :refer [pprint]]
            [belib.browser :as bb]
            [belib.spec :as bs]
            [grooob.grid-view.ui :as grid-view]
            [grooob.model.model-spec :as ms]
            [grooob.utils :as utils]
            [belib.core :as bc]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [grooob.project-details.events :as e]
            [grooob.re-comps.ui :as re-c]
            [lambdaisland.deep-diff2 :as ddiff]))

(def model-history (atom ()))
;(def counter (atom 0))

(rf/reg-event-db
  :update-task
  (fn [db [_ p-id delta-task]]
    (assoc db :model (model/update-task (:model db) p-id delta-task))))

(defn render-project-details-form [component-id _model key-down-rules]
  (let [;browser-size   (rf/subscribe [:view/browser-size])
        ;browser-scroll (rf/subscribe [:view/browser-scroll])
        _project  (rf/subscribe [:model/current-project "projects-overview-form" :data])
        _db-model (rf/subscribe [:sub/data-path [:model]])]
    (fn [component-id _model key-down-rules]
      (swap! model-history conj @_db-model)
      (let [project      (bc/sorted-map-by-keys @_project :g/sequence-num)
            p-id         (:g/entity-id project)
            sorted-tasks (model/sort-tasks-map-by-res
                           (:g/tasks project)
                           (model/resource-id-sequence @_model))
            t-id         (first (keys sorted-tasks))
            task-paths   (map (fn [task]
                                [:model :g/projects p-id :g/tasks (:g/entity-id task)])
                              (vals sorted-tasks))
            task-path    (first task-paths)
            p-name       [re-c/edit-one-field
                          {;:entity-id       p-id
                           :entity-map-path [:model :g/projects p-id]}
                          ;:entity-id-key   :g/entity-id}
                          {:field-key :g/name}]
            p-end        [re-c/edit-one-field
                          {:entity-id       p-id
                           :entity-map-path [:model :g/projects]}
                          ;:entity-id-key   :g/entity-id}
                          {:field-key  :g/end
                           :field-type "date"}]]
        [:<>
         ;[:div.select.is-small.is-fullwidth [:select [:option "res-4"] [:option "reds-1"] [:option "res-8"] [:option "res-3"] [:option "res-2"] [:option "res-6"] [:option "res-9"] [:option "res-10"] [:option "res-7"] [:option "res-5"]]]
         [:div.columns.mt-0.mb-0.pt-0.pb-0.is-mobile
          [:div.column.is-offset-2.mt-0.mb-0.pt-0.pb-0.is-mobile
           [:h1.is-6.mb-1.pl-2 "selected Project"]]]
         [re-c/edit-one-field
          true
          {;:entity-id       p-id
           :entity-map-path [:model :g/projects p-id]}
          ;:entity-id-key   :g/entity-id}
          {:field-key :g/name}]

         [re-c/edit-one-field
          true
          {:entity-id       p-id
           :entity-map-path [:model :g/projects]}
          ;:entity-id-key   :g/entity-id}
          {:field-key  :g/end
           :field-type "date"}]

         [:div.columns.mt-0.mb-0.pt-0.pb-0.is-mobile
          [:div.column.is-offset-2.mt-0.mb-0.pt-0.pb-0.is-mobile
           [:h1.is-6.mb-1.mt-2.pl-2 "first Task"]]]

         [re-c/edit-one-entity
          true
          {:entity-id       t-id
           :entity-map-path [:model :g/projects p-id :g/tasks]
           :entity-id-key   :g/entity-id}

          [{:field-key     :g/entity-id
            ;:field-type    "number"
            :field-show-as "ID"
            :disabled      true}
           ;:update-fn     [:update-task p-id]}

           {:field-key     :g/start
            :field-type    "date"
            :field-show-as "Start"
            :update-fn     [:update-task p-id]}

           {:field-key     :g/end
            :field-type    "date"
            :field-show-as "End"
            :update-fn     [:update-task p-id]}

           {:field-key     :g/resource-id
            :field-show-as "Resource"
            :field-type    "ref"
            :ref-path      [:model :g/resources]
            :ref-id        :g/entity-id
            :ref-selectors [:g/name]
            :update-fn     [:update-task p-id]}

           {:field-key     :g/capacity-need
            :field-type    "number"
            :field-show-as "Capacity"
            :update-fn     [:update-task p-id]}

           {:field-key :g/task-name
            #_:update-fn  #_[:update-task p-id]}]]


         [:div.columns.mt-0.mb-0.pt-0.pb-0.is-mobile
          [:div.column.mt-2.mb-1.pt-0.pb-0.is-mobile
           [:h1.is-6.mt-3.pl-3 "all Tasks of selected Project"]]]

         [re-c/edit-all-entities #_re-c/edit-one-entity
          true ; heading
          {;:entity-id       t-id
           :entity-map-path [:model :g/projects p-id :g/tasks]
           :entity-id-key   :g/entity-id}

          [{:field-key :g/entity-id
            ;:field-type "number"
            ;:field-show-as "ID"
            :disabled  true}

           {:field-key     :g/start
            :field-type    "date"
            :field-show-as "Start"
            :update-fn     [:update-task p-id]}

           {:field-key     :g/end
            :field-type    "date"
            :field-show-as "End"
            :update-fn     [:update-task p-id]}

           {:field-key     :g/resource-id
            :field-show-as "Resource"
            :field-type    "ref"
            ;:disabled      true
            :ref-path      [:model :g/resources]
            :ref-id        :g/entity-id
            :ref-selectors [:g/name :g/sequence-num]
            :update-fn     [:update-task p-id]}

           {:field-key     :g/capacity-need
            :field-type    "number"
            :field-show-as "Capacity"
            :update-fn     [:update-task p-id]}

           {:field-key :g/task-name
            #_:update-fn  #_[:update-task p-id]}]]

         (let [mh   @model-history
               diff (-> (ddiff/diff (second mh) (first mh))
                        ddiff/minimize)]

           [:<>
            ;[:pre @counter "  " (:g/name @_db-model)]
            [:pre "-------  DIFF:\n\n" (with-out-str (pprint diff) #_(ddiff/diff (first mh) (second mh)))]
            [:pre "-------  ALL:\n\n" (with-out-str (pprint @_db-model) #_(ddiff/diff (first mh) (second mh)))]])]))))

(defn projects-details-form [component-id
                             _model
                             key-down-rules]

  (if (or (not @_model) (= (:projects @_model) []))
    [:div (str "no model for component: " component-id)]
    (let [
          scroll-listener (atom nil)
          m-up-listener   (atom nil)
          m-down-listener (atom nil)
          m-move-listener (atom nil)]
      (reagent/create-class
        {:display-name           component-id
         :reagent-render         render-project-details-form
         :component-did-mount
         (fn []
           ;(println "View mounted for " component-id)
           (rf/dispatch [::rp/set-keydown-rules
                         key-down-rules])

           #_(reset! scroll-listener
                     (events/listen (.getElementById js/document component-id)
                                    EventType.SCROLL scroll-fn))
           #_(reset! m-up-listener
                     (events/listen js/window
                                    EventType.MOUSEUP
                                    #(reset! e/start-dragging nil)))
           #_(reset! m-down-listener
                     (events/listen js/window
                                    EventType.MOUSEDOWN
                                    (fn [event]
                                      (reset! e/start-dragging
                                              {:x (.-offsetX event)
                                               :y (.-offsetY event)}))))
           #_(reset! m-move-listener
                     (events/listen js/window
                                    EventType.MOUSEMOVE
                                    (fn [event]
                                      (when @e/start-dragging
                                        (e/dragging-debounced
                                          @e/start-dragging
                                          {:x (.-offsetX event)
                                           :y (.-offsetY event)}))))))
         :component-will-unmount (fn []
                                   ;(println "View will unmount: " component-id)
                                   (rf/dispatch [::rp/set-keydown-rules
                                                 {}])
                                   #_(events/unlistenByKey @scroll-listener)
                                   #_(events/unlistenByKey @m-up-listener)
                                   #_(events/unlistenByKey @m-down-listener)
                                   #_(events/unlistenByKey @m-move-listener))}))))

(defn project-details-view [component-id]
  (let [;cross   (rf/subscribe [:view/cross])
        ;model   (rf/subscribe [:model/model])
        _project (rf/subscribe [:model/model])
        ;id      (b/next-local-id)
        #_component-id #_(str "project-single-view-" id)]
    #_(rf/dispatch-sync [:grid-view/init component-id])
    (fn []
      [:<>
       [re-c/overview-proj-details-menu]
       #_[:div "project: " (str (last (get (vec (:projects @model)) (:project @cross))))]
       ;[:pre (with-out-str (pprint @model))]
       ;[:pre (with-out-str (pprint @project))]

       [projects-details-form
        component-id
        _project
        (e/project-details-view-keydown-rules component-id _project)]])))
