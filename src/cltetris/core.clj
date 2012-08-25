(ns cltetris.core
  ( :import (java.util Date)
            (java.awt Frame Graphics Color)
            (java.awt.event KeyListener)))

(def keysdown (ref #{}))

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
                               (dosync (alter keysdown conj :up))) 50)
        keyreleased (debounce (fn [this e]
                                (dosync (alter keysdown disj :up))) 50)]
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
        coords (case direction
                 :up [100 0]
                 :down [100 200]
                 :left [0 100]
                 :right [200 100]
                 :center [100 100]
                [100 100])]
    (let [[x y] coords]
      (doto g
        (.setColor (Color. 0 0 0))
        (.clearRect 0 0 1000 1000)
        (.fillRect x y 100 100)))))

#_(
  "Commands to manually contol the thread flag"
  (dosync (alter running (fn [& args] true)))
  (dosync (alter running (fn [& args] false)))
)

(defn -main
  "Start the show"
  [& args]
  (def frame (Frame.))
  (.show frame)
  (.setSize frame 500 500)
  (setup-frame frame)
  (def running (ref true))
  (.start (Thread. (fn [] (while @running (do
                                            (draw-square frame (first @keysdown))
                                            (Thread/sleep 100))))))
  (.start (Thread. (fn [] (while @running (do
                                            (println (first @keysdown))
                                            (Thread/sleep 100)))))))