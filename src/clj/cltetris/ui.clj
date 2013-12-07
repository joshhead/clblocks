(ns cltetris.ui
  (:require [cltetris.tetrominos :as tetrominos]
            [clojure.string :as clojure.string]
            [clojure.core.async :as async :refer [go]])
  (:import [java.awt Frame Graphics Color]
           [java.awt.event KeyListener KeyEvent WindowAdapter]
           [java.awt.image BufferedImage]))

(def keycodes {"Up" :up
               "Down" :down
               "Left" :left
               "Right" :right
               "Escape" :escape})

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
  "Returns a channel onto which will be put vectors of [direction action]
  where direction is in #{:up :down :left :right :escape} and
  action is in #{:press :release}."
  [^Frame frame]
  (let [event-chan (async/chan)
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

    event-chan))

(defn setup-close-listener
  "Returns a channel that will have :close put on it when
   the close button of a frame is clicked."
  [^Frame frame]
  (let [c (async/chan)]
    (.addWindowListener frame (proxy [WindowAdapter] []
                                (windowClosing [event]
                                  (async/put! c :close))))
    c))

(defn backing-image
  "Given a frame create a new BufferedImage.
  Get a Graphics with (.createGraphics img) and draw to that.
  When finished, write draw the backing image to the frame."
  [^Frame frame]
  (let [width (.getWidth frame)
        height (.getHeight frame)]
    (BufferedImage. width height BufferedImage/TYPE_4BYTE_ABGR)))

(defn draw-backing-image
  "Draw a BufferedImage to a Frame"
  [^Frame frame ^BufferedImage img]
  (.drawImage (.getGraphics frame) img 0 0 nil))

(defn- all-coords
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
        (when (> cell-val 0)
          (.fillRect g x y col-width row-height))))
    (draw-backing-image frame img))
  grid)

