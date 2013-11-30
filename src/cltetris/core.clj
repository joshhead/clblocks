(ns cltetris.core
  (:require [cltetris.tetrominos :as tetrominos]
            [clojure.core.async :as async])
  (:import [java.util Date]
           [java.awt Frame Graphics Color]
           [java.awt.event KeyListener KeyEvent WindowAdapter]
           [java.awt.image BufferedImage]))

(def keycodes {"Up" :up
               "Down" :down
               "Left" :left
               "Right" :right
               "Escape" :escape})

(def field-width 10)
(def field-height 22)

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
  "Returns a vector of two channels. The first will get return vectors of
  [direction action] where direction is in #{:up :down :left :right :escape} and
  action is in #{:press :release}. To remove key listeners, put a channel
  on the second channel. When it is done cleaning up, it will close the
  channel it received."
  [^Frame frame]
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

    (async/go
     (let [cancelled-chan (async/<! cancel-chan)]
       (.removeKeyListener frame key-listener)
       (async/close! event-chan)
       (async/close! cancel-chan)
       (async/close! cancelled-chan)))

    [event-chan cancel-chan]))

(defn setup-close-listener
  "Returns a channel that will have :close put on it when
   the close button of a frame is clicked."
  [^Frame frame]
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
     (let [cancelled (async/chan)]
       (async/<! closec)
       (async/>! cancel-keys cancelled)
       (async/<! cancelled)
       (.hide frame)))
    frame))

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

(defn frame-draw-square
  "Draw a square in one of 5 positions at a fixed size"
  [frame direction]
  (let [img (backing-image frame)
        g (.createGraphics img)
        width  (.getWidth frame)
        height (.getHeight frame)
        [x y]  (case direction
                 :up [50 0]
                 :down [50 100]
                 :left [0 50]
                 :right [100 50]
                 :center [50 50]
                 [50 50])]
    (doto g
      (.setColor (Color. 200 200 200))
      (.fillRect 0 0 width height)
      (.setColor (Color. 0 100 0))
      (.fillRect x y 50 50))

    (draw-backing-image frame img)))

(defn make-grid
  "of: what to make a grid of e.g. 10x10 grid of 0's (make-grid 0 10 10)"
  [of cols rows]
  (let [row (vec (take cols (repeat of)))]
    (vec (take rows (repeat row)))))

(defn random-grid
  "Generate a new grid of the given size filled randomly with 1's and 0's"
  [cols rows]
  (let [row (fn [] (vec (take cols (repeatedly #(rand-int 2)))))]
    (vec (take rows (repeatedly row)))))

(defn print-grid
  "Print a grid as one row per line with no spaces"
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
        (when (> cell-val 0)
          (.fillRect g x y col-width row-height))))
    (draw-backing-image frame img))
  grid)

(defn empty-row
  [length]
  (vec (take length (repeat 0))))

(defn expand-row
  "row: vector to expand
  length: length to expand to, padded with 0's
  offset: precede with this many 0's. negative offsets take from the head"
  [row length offset]
  (let [before (if (pos? offset) (take offset (repeat 0)) [])
        middle (if (pos? offset) row (drop (- offset) row))
        after (take (- length offset (count row)) (repeat 0))]
    (vec (take length (concat before middle after)))))

(defn expand-grid
  [grid rows cols [row col :as offset]]
  (let [before (take row (repeatedly #(empty-row cols)))
        middle (map #(expand-row % cols col) grid)
        after (take (- rows row (count grid)) (repeatedly #(empty-row cols)))]
    (vec (concat before middle after))
))

(defn merge-grid
  [main sub [row col :as offset]]
  (let [rows (count main)
        cols (count (first main))
        expanded-sub (expand-grid sub rows cols offset)]
    (vec (map #(vec (map + %1 %2)) main expanded-sub))))

(defn overlapping?
  "True if the game piece overlaps filled space on the grid at its current position"
  [game]
  (let [grid (merge-grid (:grid game) (:piece game) (:position game))]
    (some (partial < 1) (apply concat grid))))

(defn lock-piece
  [{:keys [grid piece position] :as game}]
  (assoc game :grid (merge-grid grid piece position)))

(defn activate-next
  [{:keys [piece next] :as game}]
  (-> game
      (assoc :piece next)
      (assoc :next (tetrominos/random))
      (assoc :position [0 0])))

(defn n-rows-dirty-grid
  [n]
  (vec (concat
        (make-grid 0 field-width (- field-height n))
        (random-grid field-width n))))

(defn new-game
  []
  {:grid (n-rows-dirty-grid 3)
   :position [0 0]
   :piece (tetrominos/random)
   :next (tetrominos/random)})

(defn out-of-bounds?
  "True if :position puts all or part of :piece outside of :grid"
  [game]
  false)

(defn move-clockwise
  [game]
  (update-in game [:piece] tetrominos/rotate))

(defn move-right
  [game]
  (update-in game [:position 1] inc))

(defn move-left
  [game]
  (update-in game [:position 1] dec))

(defn move-down
  [game]
  (let [moved (update-in game [:position 0] inc)]
    (if (overlapping? moved)
      (-> game lock-piece activate-next)
      moved)))

(defn step-game
  "Advance game one frame"
  [game input]

  (case input
    :up (move-clockwise game)
    :left (move-left game)
    :right (move-right game)
    :down (move-down game)
    game))

(defn tick-chan
  [ms]
  (let [tickc (async/chan)]
    (async/go
     (loop []
       (async/<! (async/timeout ms))
       (async/>! tickc :tick)
       (recur)))
    tickc))

(defn play
  []
  (let [frame (new-frame 200 440)
        [keys cancel-keys] (setup-key-listener frame)
        ticker (tick-chan 500)
        closec (setup-close-listener frame)
        quitc (async/chan)]

    (async/go (loop [game (new-game)]
                (let [[val port] (async/alts! [keys ticker])
                      key (if (= port keys) val [:down :press])]
                  (when (= (key 1) :press)
                    (let [next-game (step-game game (key 0))]
                      (frame-draw-grid frame (merge-grid (:grid next-game) (:piece next-game) (:position next-game)))))
                  (when-not (or (nil? key) (= (key 0) :escape))
                    (if (= (key 1) :press)
                      (recur (step-game game (key 0)))
                      (recur game)))))
              (async/>! closec :quit))

    (async/go
     (let [cancelled (async/chan)]
       (async/<! closec)
       (.hide frame)
       (async/close! quitc)))

    quitc))

(defn -main
  [& args]
  (async/<!! (play))
  (System/exit 0))

(comment
  (demo-events)
  (play)
)

