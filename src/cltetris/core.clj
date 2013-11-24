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

(defn backing-image
  [frame]
  (let [width (.getWidth frame)
        height (.getHeight frame)]
    (BufferedImage. width height BufferedImage/TYPE_4BYTE_ABGR)))

(defn draw-backing-image
  [frame img]
  (.drawImage (.getGraphics frame) img 0 0 nil))

(defn frame-draw-square
  [frame direction]
  (let [img (backing-image frame)
        g (.createGraphics img)
        width  (.getWidth frame)
        height (.getHeight frame)
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
      (.fillRect x y 100 100))

    (draw-backing-image frame img)))

(defn make-grid
  "of: what to make a grid of e.g. 10x10 grid of 0's (make-grid 0 10 10)"
  [of cols rows]
  (let [row (vec (take cols (repeat of)))]
    (vec (take rows (repeat row)))))

(defn random-grid
  [cols rows]
  (let [row (fn [] (vec (take cols (repeatedly #(rand-int 2)))))]
    (vec (take rows (repeatedly row)))))

(defn print-grid
  [grid]
  (doseq [row grid]
    (println (apply str row)))
  grid)

(defn all-coords
  "For a given width and height, or a grid of a given width and height,
  return a list of vectors [row col] for every coordinate on the grid."
  ([grid]
     (let [rows (count grid)
           cols (count (first grid))]
       (all-coords cols rows)))
  ([rows cols]
     (for [y (range rows) x (range cols)]
       (vector x y))))

(defn frame-draw-grid
  "Draw a grid stretched to fit a java.awt.Frame"
  [frame grid]
  (let [img (backing-image frame)
        g (.createGraphics img)
        width  (.getWidth frame)
        height (.getHeight frame)
        rows (count grid)
        cols (count (first grid))
        row-height (/ height rows)
        col-width (/ width cols)]
    (.setColor g (Color. 200 200 200))
    (.fillRect g 0 0 width height)
    (doseq [[row col :as coord] (all-coords grid)]
      (let [x (* col-width  col)
            y (* row-height row)
            cell-val (get-in grid coord)]
        (.setColor g (Color. 0 80 0))
        (when (= cell-val 1)
          (.fillRect g x y col-width row-height))))
    (draw-backing-image frame img))
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
    (frame-draw-square frame :center)
    (async/go (loop []
                (let [key (async/<! keys)]
                  (if (= (key 1) :press)
                    (frame-draw-square frame (key 0))
                    (frame-draw-square frame :center))
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

