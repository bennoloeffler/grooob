(ns re-pipe.playback
  (:require [playback.core]))

; TODO: move to belib.playback?

; TODO: not needed? just require re-pipe.playback should do it
#_(defn load
    "Just make playback #> #>> #><[] available."
    [])

; #>      ; trace output
; #>>     ; trace output and input/bindings/steps (depending on the form)
; #>< _   ; reference currently selected portal data #><[]
; #>(defn ; makes functions replay with cached data on reload
; #>(defmethod,
; #>(>defn ;guardrails see:
(comment

  ; just for testing and remembering ;-)

  ; 1. eval in repl
  (require 'playback.preload) ; open the portal

  ; 2. eval in repl
  #>>(defn make-something [aa bbb]
       #> (let [a (/ aa 2)
                b (/ bbb 3)]
            #>> (->> (range (* a b))
                     (map inc)
                     ; 4. try change the 3 to 2 and reload (eval in repl)
                     (map #(* 3 %))
                     (map str))))

  ; 3. eval in repl
  (make-something 2 3)

  ; 5. select some data in portal and eval this...
  (println #><[])

  nil)