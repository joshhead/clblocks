(ns cltetris.core
  ( :import (java.util Date)
            (java.awt Frame Graphics Color)
            (java.awt.event KeyListener)))

(def frame (Frame.))

(defn remove-key-listeners
  [component]
  (for [l (.getKeyListeners component)]
    (.removeKeyListener component l)))

(defn setup-frame
  [frame]
  (let [last-press (atom (.getTime (Date.)))
        key-listener (reify
                       java.awt.event.KeyListener
                       (keyPressed [this e]
                         (let [start @last-press
                               end (.getTime (Date.))
                               diff (- end start)]
                           (when (> diff 50) (println "keypress" diff e))
                           (compare-and-set! last-press start end)))
                       (keyReleased [this e]
                         nil)
                       (keyTyped [this e]
                         nil))]
    (.addKeyListener frame key-listener)))

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

