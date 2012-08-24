(ns cltetris.core
  ( :import (java.awt Frame Graphics Color)
            (java.awt.event KeyListener)))

(def frame (Frame.))

(defn remove-key-listeners
  [component]
  (for [l (.getKeyListeners component)]
    (.removeKeyListener component l)))

(defn setup-frame
  [frame]
  (let [key-listener (reify
                       java.awt.event.KeyListener
                       (keyPressed [this e]
                         (println "keypress" e))
                       (keyReleased [this e]
                         (println "keyrelease" e))
                       (keyTyped [this e]
                         (println "keytyped" e)))]
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

