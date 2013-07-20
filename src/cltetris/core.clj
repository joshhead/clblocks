(ns cltetris.core
  ( :import (java.util Date)
            (java.awt Frame Graphics Color)
            (java.awt.event KeyListener KeyEvent WindowAdapter)
            (java.awt.image BufferedImage)))

(defonce keysdown (ref #{}))
(defonce running (ref true))

(defn remove-key-listeners
  [component]
  (for [l (.getKeyListeners component)]
    (.removeKeyListener component l))
  component)

(defn setup-frame
  [frame]
  (remove-key-listeners frame)
  (let [keycodes {"Up" :up, "Down" :down, "Left" :left, "Right" :right}
        get-event-keyword (fn [e] (get keycodes (KeyEvent/getKeyText (.getKeyCode e)) :center))
        keypressed (fn [this e]
                     (dosync (alter keysdown conj (get-event-keyword e))))
        keyreleased (fn [this e]
                      (dosync (alter keysdown disj (get-event-keyword e))))
        key-listener (reify
                       java.awt.event.KeyListener
                       (keyPressed [this e]
                         (keypressed this e))
                       (keyReleased [this e]
                         (keyreleased this e))
                       (keyTyped [this e]
                         nil))
        close-listener (proxy [WindowAdapter] []
                         (windowClosing
                           [event]
                           (dosync (alter running (constantly false)))
                           (System/exit 0)))]
    (doto frame
      (.addKeyListener key-listener)
      (.addWindowListener close-listener))))

(defn buffered-draw
  "Accepts a frame and a drawing function.
  Draws on a backing graphic and then copies
  the backing graphic to the frame.

  Backing graphic will have clipRect set to the
  size of the frame.

  (buffered-draw frame (fn [g] (draw-stuff g)))"
  [frame f]
  (let [width    (.getWidth frame)
        height   (.getHeight frame)
        back-img (BufferedImage. width height BufferedImage/TYPE_4BYTE_ABGR)
        back-g   (.createGraphics back-img)
        g        (.getGraphics frame)]
    (.setClip back-g 0 0 width height)
    (f back-g)
    (.drawImage g back-img 0 0 nil))
  frame)

(defn draw-square
  [g direction]
  (let [width  (.-width  (.getClipBounds g))
        height (.-height (.getClipBounds g))
        [x y]  (case direction
                 :up [100 0]
                 :down [100 200]
                 :left [0 100]
                 :right [200 100]
                 :center [100 100]
                 [100 100])]
    (doto g
      (.setColor (Color. 200 200 200))
      (.fillRect 0 0 width height)
      (.setColor (Color. 0 100 0))
      (.fillRect x y 100 100))))

(defn -main
  "Start the show"
  [& args]
  (defonce frame (Frame.))
  (.show frame)
  (.setSize frame 300 300)
  (setup-frame frame)
  (dosync (alter running (constantly true)))
  (let [bdf (partial buffered-draw frame)]
    (.start (Thread. (fn [] (while @running (do
                                              (bdf #(draw-square % (first @keysdown)))
                                              (Thread/sleep 33))))))))

(comment
  "Commands to manually contol the thread flag"
  (dosync (alter running (constantly true)))
  (dosync (alter running (constantly false)))

  (.start (Thread. (fn [] (while @running (do
                                            (println (first @keysdown))
                                            (Thread/sleep 33))))))
  "Close frame"
  (doto frame
    remove-key-listeners
    .hide)
)

