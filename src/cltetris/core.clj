(ns cltetris.core
  (:require [clojure.core.async :as async])
  (:import [java.util Date]
           [java.awt Frame Graphics Color]
           [java.awt.event KeyListener KeyEvent WindowAdapter]
           [java.awt.image BufferedImage]))

(def keycodes {"Up" :up
               "Down" :down
               "Left" :left
               "Right" :right})

(defn new-frame
  ([]
     (new-frame 200 440))
  ([width height]
     (let [frame (Frame.)]
       (doto frame
         (.setSize width height)
         (.show)))))

(defn get-event-keyword
  [e]
  (get keycodes (KeyEvent/getKeyText (.getKeyCode e)) nil))

(defn setup-key-listener
  [frame]
  (let [event-chan (async/chan)
        cancel-chan (async/chan)
        key-listener (reify
                       java.awt.event.KeyListener
                       (keyPressed [_ e]
                         (when-let [event-kw (get-event-keyword e)]
                           (async/put! event-chan [event-kw :press])))
                       (keyReleased [_ e]
                         (when-let [event-kw (get-event-keyword e)]
                           (async/put! event-chan [event-kw :release])))
                       (keyTyped [this e]
                         nil))]
    (.addKeyListener frame key-listener)

    ; On writing to or closing of cancel-chan, remove key listener
    (async/go
     (async/<! cancel-chan)
     (async/close! cancel-chan)
     (.removeKeyListener frame key-listener))

    [event-chan cancel-chan]))

(defn setup-close-listener
  [frame]
  (let [c (async/chan)]
    (.addWindowListener frame (proxy [WindowAdapter] []
                                (windowClosing [event]
                                  (async/put! c :close))))
    c))

(defn demo-events
  "Create a frame and listen for events, printing them to stdout"
  []
  (let [frame (new-frame)
        [keys cancel-keys] (setup-key-listener frame)
        closec (setup-close-listener frame)]
    (async/go (loop []
                (let [key (async/<! keys)]
                  (println key)
                  (when-not (nil? key)
                    (recur)))))
    (async/go
     (async/<! closec)
     (async/close! cancel-keys)
     (.hide frame))))

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
  (let [frame (new-frame 300 300)
        bdf (partial buffered-draw frame)
        [keys cancel-keys] (setup-key-listener frame)
        closec (setup-close-listener frame)]
    (bdf #(draw-square % :center))
    (async/go (loop []
                (let [key (async/<! keys)]
                  (if (= (key 1) :press)
                    (bdf #(draw-square % (key 0)))
                    (bdf #(draw-square % :center)))
                  (when-not (nil? key)
                    (recur)))))
    (async/go
     (async/<! closec)
     (async/close! cancel-keys)
     (.hide frame))))

(comment
  (demo-events)
  (-main)
)

