(ns grooob.comps.scenes-basic
  (:require
    [portfolio.reagent :refer-macros [defscene]]
    [clojure.string :as str]
    [grooob.comps.ui]
    [belib.malli :as bm]
    [grooob.model.model-malli :as mm]
    [belib.date-time :as bd]
    [re-frame.core :as rf]
    [tick.core :as t]
    [reagent.core :as r]))



(def data (r/atom {:data "shared text"}))

(defscene input-field-simple
          ^{:key 1}
          [grooob.comps.ui/input-field data :data "text" "Your Input"])

(defscene input-field-date
          ^{:key 2}
          [grooob.comps.ui/input-field (atom {}) :data "date" "Your Date"])

(defscene input-field-date-data
          ^{:key 21}
          [grooob.comps.ui/input-field (atom {:data "2023-12-24"}) :data "date" "Your Date"])


(defscene input-field-icon
          ^{:key 3}
          [grooob.comps.ui/input-field (atom {}) :data "text" "Your Input"
           "fa-user"])

(defscene input-field-constrained
          ^{:key 4}
          [:<> [grooob.comps.ui/input-field (atom {:data "some text"}) :data "text" "Your Input in UPPERCASE" "fa-arrow-down-a-z"
                (fn nil-if-no-errors-vec-of-str-if-errors [data]
                  (when (and data
                             (not= data (str/upper-case data)))
                    ["please type uppercase!"]))]
           [:div "."]
           [:div "."]
           [:div "⬆️ errors come here"]])

(defscene input-field-constrained-validate-at-start
          ^{:key 5}
          [grooob.comps.ui/input-field (atom {:data "the"}) :data "text" "Your Input in UPPERCASE" "fa-arrow-down-a-z"
           (fn nil-if-no-errors-vec-of-str-if-errors [data]
             (println data ", " (count data))
             (let [errors []
                   errors (if (and data (not= data (str/upper-case data)))
                            (conj errors "Please type uppercase.")
                            errors)
                   errors (if (<= (count data) 3)
                            (conj errors "Type more than 3 characters")
                            errors)]
               (seq errors)))

           true])


(comment (str/upper-case "abc"))

(defscene input-field-email
          ^{:key 6}
          [grooob.comps.ui/input-field (atom {}) :data "email" "Your Email" "fa-envelope"
           (fn [data] (bm/hum-err bm/email-schema data))])

(defscene password-field
          [:<>
           ^{:key 7}
           [grooob.comps.ui/input-password-field (atom {}) :pw "Passwortttt"]
           [:div "."]
           [:div "."]
           [:div "⬆️ errors come here"]])

(defscene login-field-effect-bounce

          (let [bounce-off (grooob.comps.ui/fa-bounce-off)]
            ^{:key 8}
            [grooob.comps.ui/input-field (atom {}) :data "text" "user" [bounce-off "fa-2x" "fa-user"]]))

(defscene login-field-effect-smaller
          ^{:key 9}
          [grooob.comps.ui/input-field (atom {})
           :data
           "text"
           "USER"
           [(grooob.comps.ui/fa-smaller) "fa-user"]])

