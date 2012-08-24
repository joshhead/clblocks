(ns cltetris.core
  ( :import (java.util Date)
            (java.awt Frame Graphics Color)
            (java.awt.event KeyListener)))

(def frame (Frame.))

(defn debounce
  "Return a function that will call f only once until not called for ms milliseconds"
  [f ms]
  (let [last-call (atom (.getTime (Date.)))]
    (fn [& args]
      (let [start @last-call
            end (.getTime (Date.))
            diff (- end start)]
        (when (> diff ms) (apply f args))
        (compare-and-set! last-call start end)))))

(defn remove-key-listeners
  [component]
  (for [l (.getKeyListeners component)]
    (.removeKeyListener component l)))

(defn setup-frame
  [frame]
  (let [keypressed (debounce (fn [this e]
                               (println "keypress")) 50)
        keyreleased (debounce (fn [this e]
                                (println "keyrelease")) 50)]
    (let [key-listener (reify
                         java.awt.event.KeyListener
                         (keyPressed [this e]
                           (keypressed this e))
                         (keyReleased [this e]
                           (keyreleased this e))
                         (keyTyped [this e]
                           nil))]
      (.addKeyListener frame key-listener))))

(defn draw-square
  [frame direction]
  (let [g (.getGraphics frame)
        left [0 100]
        right [200 100]
        up [100 0]
        down [100 200]
        center [100 100]
        coords (case direction
                 :up up
                 :down down
                 :left left
                 :right right
                 :center center)]
    (let [[x y] coords]
      (doto g
        (.clearRect 0 0 1000 1000)
        (.fillRect x y 100 100)))))

(defn -main
  "I don't do a whole lot."
  [& args]
  (println "Hello, World!"))

