(ns cltetris.core
  (:import (java.util Date)
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

(defn make-grid
  [of cols rows]
  {:cols cols
   :rows rows
   :data (into [] (take (* rows cols) (repeat of)))})

(defn random-grid
  [cols rows]
  {:cols cols
   :rows rows
   :data (into [] (take (* rows cols) (repeatedly #(rand-int 2))))})

(defn print-grid
  [{:keys [cols data] :as grid}]
  (doseq [row (partition cols data)]
    (println (apply str row)))
  grid)

(defn all-coords
  ([{:keys [rows cols]}]
   (all-coords rows cols))
  ([rows cols]
    (for [y (range rows) x (range cols)]
      (vector x y))))

(defn cell-coords
  [grid]
  (map (fn [cell coord] {:cell cell :coord coord}) (:data grid) (all-coords grid)))

(defn draw-grid
  [g {:keys [rows cols data] :as grid}]
  (let [width  (.-width  (.getClipBounds g))
        height (.-height (.getClipBounds g))
        row-height (/ height rows)
        col-width (/ width cols)]
    (.setColor g (Color. 200 200 200))
    (.fillRect g 0 0 width height)
    (doseq [{cell :cell [cell-x cell-y] :coord} (cell-coords grid)]
      (let [x (* col-width  cell-x)
            y (* row-height cell-y)]
        (.setColor g (Color. 0 80 0))
        (when (> 0 cell)
          (.fillRect g x y col-width row-height)))))
  grid)

(defn offset
  [[x-offset y-offset] coords]
  (map (fn [[x y]] [(+ x x-offset) (+ y y-offset)]) coords))

(defn merge-grid
  [main sub [x y] cell-merge-fn]
  main)

(defn -main
  "Start the show"
  [& args]
  (defonce frame (Frame.))
  (.show frame)
  (.setSize frame 200 440)
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

