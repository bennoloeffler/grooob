(ns ^:dev/once grooob.app
  (:require
    [grooob.core :as core]
    [cljs.spec.alpha :as s]
    [expound.alpha :as expound]
    [devtools.core :as devtools]
    [grooob.debug.playback]))

(extend-protocol IPrintWithWriter
  js/Symbol
  (-pr-writer [sym writer _]
    (-write writer (str "\"" (.toString sym) "\""))))

(set! s/*explain-out* expound/printer)

(enable-console-print!)

(devtools/install!)

(core/init!)
