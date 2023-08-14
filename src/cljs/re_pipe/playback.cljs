(ns re-pipe.playback
  (:require [playback.core]))

(defn load
  "Just make playback #> #>> #><[] available."
  [])

(comment
  ; mini-doc see user.clj
  (require 'playback.preload)

  ; test #> #>> #><[]
  #>>(defn make-something [a b]
       (->> (range (* a b))
            (map inc)
            (map #(* 3 %))
            (map str)))
  (make-something 3 3))