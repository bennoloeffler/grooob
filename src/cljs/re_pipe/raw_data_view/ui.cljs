(ns re-pipe.raw-data-view.ui
  (:require [re-frame.core :as rf]
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
            [re-pipe.grid-view.ui :as grid-view]
            [re-pipe.model.model-spec :as ms]
            [re-pipe.utils :as utils]
            [ajax.core :as ajax]
            [goog.history.EventType :as HistoryEventType]
            [time-literals.read-write]
            [luminus-transit.time :as time]
            [cognitect.transit :as transit]
            [re-pressed.core :as rp]
            [re-pipe.projects-overview.events :as e]
            [re-pipe.re-comps.ui :as re-c]))

(defn raw-data-form []
  (let [;cross    (rf/subscribe [:view/cross])
        ;model   (rf/subscribe [:model/model])
        _model       (rf/subscribe [:sub/data-path [:model]])
        _data-buffer (reagent/atom (with-out-str (pprint (:g/resources @_model))))
        ;id      (b/next-local-id)
        #_component-id #_(str "project-single-view-" id)]
    #_(rf/dispatch-sync [:grid-view/init component-id])
    (fn []
      [:<>
       [re-c/overview-proj-details-menu]
       [:div "project-raw-data" #_(str (last (get (vec (:projects @_project)) (:y @cross))))]
       #_[:pre (with-out-str (pprint @_model))]

       [:button.button "save"]
       ; TODO remove load, transform
       [:textarea.textarea {:placeholder "the raw model here"
                            :width       "100%"
                            :heigth      "100%"
                            :rows        "20"
                            :value       @_data-buffer
                            :on-change   #(let [val (-> % .-target .-value)]
                                            (reset! _data-buffer val)
                                            (println "SET VAL:")
                                            (println val))}]


       ;[:pre (with-out-str (pprint @project))]

       #_[projects-details-form
          component-id
          _project
          (e/project-details-view-keydown-rules component-id _project)]])))

