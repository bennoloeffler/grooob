(ns grooob.load-model-from-file
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [belib.date-parse-messy :as parse-messy]
            [belib.core :as bc]
            [belib.date-time :as bd]
            [tick.core :as t]
            [hyperfiddle.rcf :refer [tests]]
            [belib.test :as bt :refer [expect-ex return-ex]]))

(hyperfiddle.rcf/enable! false)


(comment ; simples insta parser

  (def as-and-bs
    (insta/parser
      "S = AB*
       AB = A B
       A = 'a'+
       B = 'b'+"))

  (as-and-bs "aaaaabbbaaaabb")

  :end)


(defn humanize-insta-error
  "works in the special case, when the insta tokens are build from regexes.
  Start those with e.g. (___whitespace___)*
  The text between ___ is used as error message.
  :insta-error contains the message
  :insta-where contains a string with the position
  :insta-parsed contains the original parsing result
  "
  [insta-parsed]
  (if (insta/failure? insta-parsed)
    {:insta-parsed insta-parsed
     :insta-error  (->> (insta/get-failure insta-parsed)
                        :reason
                        first
                        :expecting
                        str
                        (re-find #"___(.*)___")
                        second)
     :insta-where  (first
                     (str/split
                       (with-out-str (println (insta/get-failure insta-parsed)))
                       #"Expected:"))}
    {:insta-parsed insta-parsed
     :insta-error  nil
     :insta-where  nil}))

(defn transform-insta-to-map-new
  "Transform an instaparse result to a map - if there were no errors.

  Example:

  parameter:
    text: \"p1 22.04.1999 22.12.2323 res1 454.0  abc\")\n
    insta-parser: Projekt-Start-End-Abt-Kapa
    type-keyword: :task

  result:
  {:insta-parsed [:projects
                  [:task
                   [:project-name \"p1\"]
                   [:start \"22.04.1999\"]
                   [:end \"22.12.2323\"]
                   [:resource \"res1\"]
                   [:capa \"454.0  \"]
                   [:comment \"abc\"]]]
   :insta-error nil
   :insta-where nil
   :transformed [{:project-name \"p1\"
                  :start \"22.04.1999\",
                  :end \"22.12.2323\",
                  :resource \"res1\",
                  :capa-need \"454.0\",
                  :comment \"abc\"}]}

  "
  [text insta-parser type-keyword]
  (let [transformed (atom [])
        parsed      (insta-parser #_Projekt-Start-End-Abt-Kapa
                      text #_(slurp file-path-str #_"bsp-daten/bsp-00-nur-tasks/Projekt-Start-End-Abt-Kapa.txt"))
        data        (humanize-insta-error parsed)]
    (if-not (:insta-error data)
      (do
        (vec (clojure.walk/prewalk (fn [x]
                                     (when (and (vector? x)
                                                (> (count x) 1)
                                                (= (first x) type-keyword)
                                                (vector? (second x)))
                                       ;(println x)
                                       (let [line-as-map (into {} (drop 1 x))
                                             trimmed-map (into {} (for [[k v] line-as-map] [k (str/trim v)]))]

                                         (swap! transformed
                                                conj trimmed-map)))
                                     x)
                                   parsed))
        (merge data {:transformed @transformed}))
      data)))

(comment
  (into {} (drop 1 [:task [:project-name "p1"] [:start "13.01.2020"] [:end "19.01.2020"] [:resource "E-Kon"] [:capa "20.0"]])))

(def Projekt-Start-End-Abt-Kapa
  (insta/parser
    ;; lookahead at end (?=[\s]+) (?=[\s]?)
    "
    projects = (empty-line | task)*
    task = project-name <whitespace> start <whitespace> end <whitespace> resource <whitespace> capa-need <whitespace?> comment? <end-of-line>
    whitespace = #'(___whitespace___)*[\\t ]+'
    project-name = #'(___project_name_as_STRING___)*[\\t ]*[\\S]+'
    start = #'(___start_as_DATE___)*[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}(?=[\\s]{1})'
    end = #'(___end_as_DATE___)*[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}(?=[\\s]{1})'
    resource = #'(___resource_as_STRING___)*[\\S]+'
    capa-need = #'(___capa_need_as_NUMBER___)*[0-9]+\\.?[0-9][\\t ]*'
    comment = #'(___comment_as_STRING___)*[\\S]+[\\t ]*'
    end-of-line = #'(___end_of_line___)*[\\n]*'
    empty-line = #'(___empty_line___)*[\\s]*[\\n]*'
    "))

;; line by line could be more precise, because of some trouble with detecting line feed
#_(with-open [rdr (io/reader "bsp-daten/bsp-00-nur-tasks/Projekt-Start-End-Abt-Kapa.txt")]
    (let [lines (line-seq rdr)]
      (mapv Projekt-Start-End-Abt-Kapa lines)))


(comment
  (def data-err (let [insta-data (transform-insta-to-map-new "p1 22.04.1999 22.12.2323 res1 x454.0  abc" Projekt-Start-End-Abt-Kapa :task)]
                  (println (str "WHERE:\n" (str/trim (:insta-where insta-data))))
                  (println "ERROR: " (:insta-error insta-data))
                  insta-data))

  (insta/failure? (:insta-parsed data-err))

  (Projekt-Start-End-Abt-Kapa "p1 22.04.1999 22.12.2323 res1 454.0  abc")

  (def parsed (Projekt-Start-End-Abt-Kapa
                (slurp "bsp-daten/bsp-00-nur-tasks/Projekt-Start-End-Abt-Kapa.txt")
                :total true))

  (first (str/split (with-out-str (println (insta/get-failure (:insta-parsed data-err))))
                    #"Expected:"))

  (def data-ok (let [insta-data (transform-insta-to-map-new "p1 22.04.1999 22.12.2323 res1 454.0  abc" Projekt-Start-End-Abt-Kapa :task)]
                 (println (str "RAW DATA:\n" (:insta-parsed insta-data)))
                 (println "TRANSFORMED: " (:transformed insta-data))
                 insta-data))

  (clojure.walk/postwalk-demo data-ok)

  (clojure.walk/postwalk (fn [x]
                           (println x)
                           x)
                         parsed)

  instaparse.gll.Failure

  :end)

(comment
  (transform-insta-to-map-new
    (slurp "bsp-daten/bsp-00-nur-tasks/Projekt-Start-End-Abt-Kapa.txt")
    Projekt-Start-End-Abt-Kapa
    :task)

  (def test-str
    "p1                    27.01.2020  29.02.2020  IBN     677\n
     pro2                  06.02.2020  22.02.2020  M-Kon    34.6\n")
  (transform-insta-to-map-new
    test-str
    Projekt-Start-End-Abt-Kapa
    :task)

  :end)


(defn get-tasks [^String tasks]
  (transform-insta-to-map-new
    tasks
    Projekt-Start-End-Abt-Kapa
    :task))


(tests

  "two lines - with linefeed"
  (-> "p1                    27.01.2020  29.02.2020  IBN     677
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6 comment1_2\n"
      get-tasks
      :transformed)
  := [{:project-name "p1",
       :start        "27.01.2020",
       :end          "29.02.2020",
       :resource     "IBN",
       :capa-need    "677",}

      {:project-name "pro2",
       :start        "06.02.2020",
       :end          "22.02.2020",
       :resource     "M-Kon",
       :capa-need    "34.6",
       :comment      "comment1_2"}]

  "simple line with simple spaces at the end"
  (-> "p1 22.04.1999 22.12.2323 res 454.0 abc  "
      get-tasks
      :insta-error) := nil

  "two lines - without \n"
  (-> "p1                    27.01.2020  29.02.2020  IBN     677
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6"
      get-tasks
      :insta-error) := nil

  "free lines"
  (-> "p1                    27.01.2020  29.02.2020  IBN     677 opt_opt2
  \n

  \n\n
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6\n"
      get-tasks
      :insta-error) := nil


  "simple line with simple spaces - missing resource"
  (-> "p1 22.04.1999 22.12.2323 454.0 abc "
      get-tasks
      :insta-error) := "capa_need_as_NUMBER"

  "data between dates"
  (-> "p1                    27.01.2020  X 29.02.2020  IBN     677
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6\n"
      get-tasks
      :insta-error) := "end_as_DATE"

  "wrong start-date"
  (-> "p1                    X27.01.2020  29.02.2020  IBN     677\n
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6\n"
      get-tasks
      :insta-error) := "start_as_DATE"

  "wrong end-date"
  (-> "p1                    27.01.2020  X29.02.2020  IBN     677\n
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6\n"
      get-tasks
      :insta-error) := "end_as_DATE"

  "wrong capa"
  (-> "p1                    27.01.2020  29.02.2020  IBN     d677\n
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6\n"
      get-tasks
      :insta-error) := "capa_need_as_NUMBER"

  "double comment"
  (-> "p1                    27.01.2020  29.02.2020  IBN     677 opt opt2\n
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6\n"
      get-tasks
      :insta-error) := "whitespace"

  "double proj name"
  (-> "p1 und mehr                    27.01.2020  29.02.2020  IBN     677 opt opt2\n
       pro2                  06.02.2020  22.02.2020  M-Kon    34.6\n"
      get-tasks
      :insta-error) := "start_as_DATE"


  :end)

(def Projekt-Liefertermin
  (insta/parser
    ;; lookahead at end (?=[\s]+) (?=[\s]?)
    "
    projects = (empty-line | liefertermin-zeile)*
    liefertermin-zeile = project-name <whitespace> liefertermin <end-of-line>
    whitespace = #'(___whitespace___)*[\\t ]+'
    project-name = #'(___project_name_as_STRING___)*[\\t ]*[\\S]+'
    liefertermin = #'(___liefertermin_as_DATE___)*[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}'
    end-of-line = #'(___end_of_line___)*[\\n]*'
    empty-line = #'(___empty_line___)*[\\s]*[\\n]*'
    "))

(tests


  "read liefertermine from file"

  (-> (transform-insta-to-map-new
        (slurp "bsp-daten/bsp-01-tasks-mit-projekt-liefertermin/Projekt-Liefertermin.txt")
        Projekt-Liefertermin
        :liefertermin-zeile)
      :transformed)
  := [{:project-name "p1", :liefertermin "29.02.2020"}
      {:project-name "pro2", :liefertermin "29.03.2020"}
      {:project-name "Neu-Ulm-Anlage", :liefertermin "06.05.2020"}]


  "liefertermine wrong data"

  (-> (transform-insta-to-map-new
        "p1 29.02.2020
     pro2 29.03.2020
     Neu-Ulm-Anlage 06.05.2020 dfdf"
        Projekt-Liefertermin
        :liefertermin-zeile)
      :insta-error)
  := "whitespace"


  "liefertermine unparsable date"

  (-> (transform-insta-to-map-new
        "p1 29.02.2020
     pro2 29.03X.2020
     Neu-Ulm-Anlage 06.05.2020"
        Projekt-Liefertermin
        :liefertermin-zeile)
      :insta-error)
  := "liefertermin_as_DATE"


  "insta-parse data needs to be trimmed"

  (-> (transform-insta-to-map-new
        "p1 29.02.2020
     pro2 29.03.2020
     Neu-Ulm-Anlage 06.05.2020"
        Projekt-Liefertermin
        :liefertermin-zeile)
      :insta-parsed)
  := [:projects
      [:liefertermin-zeile [:project-name "p1"] [:liefertermin "29.02.2020"]]
      [:liefertermin-zeile [:project-name "     pro2"] [:liefertermin "29.03.2020"]]
      [:liefertermin-zeile [:project-name "     Neu-Ulm-Anlage"] [:liefertermin "06.05.2020"]]]

  :end)


(def test-capa-data {:public-holidays ["2023-10-03" "2023-10-03"]

                     :reduced-weeks   [{:week "2023-W24" :percent 60}
                                       {:week "2023-W25" :percent 60}]

                     :resources       {"M-Kon" [{:yellow 23 :red 100} ;; first has no start
                                                {:start "2023-W29" :yellow 40 :red 200}
                                                {:start "2023-W34" :yellow 30 :red 150}
                                                {:week "2023-W52" :percent 60}]}})
(def test-capa-str (pr-str test-capa-data))

(comment
  (read-string test-capa-str))

"
---
Kapa_Gesamt:
  Feiertage:
  -  \"1.1.2020 \"
  -  \"10.04.2020 \"
  -  \"13.04.2020 \"
  -  \"1.5.2020 \"
  -  \"21.5.2020 \"
  -  \"1.6.2020 \"
  -  \"3.10.2020 \"
  -  \"25.12.2020 \"
  Kapa_Profil:
     \"2020-W21 \": 50
     \"2020-W22 \": 50
Kapa_Abteilungen:
  M-Kon:
    Kapa:
      gelb: 200
      rot: 400
    Kapa_Profil:
      \"2020-W01\": 100
      \"2020-W02\": 100
      \"2020-W34\":
        gelb: 120
        rot: 150
      \"2020-W35\":
        gelb: 200
        rot: 400
  E-Kon:
    Kapa:
      gelb: 160
      rot: 200
    Kapa_Profil: {}
  SW:
    Kapa:
      gelb: 340
      rot: 460
  Mon:
    Kapa:
      gelb: 160
      rot: 400
  IBN:
    Kapa:
      gelb: 100
      rot: 200
"