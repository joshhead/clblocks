(ns cltetris.game
  (:require [clojure.string]))

(def i-tetromino
  [[0 1 0]
   [0 1 0]
   [0 1 0]
   [0 1 0]])

(def j-tetromino
  [[1 1 1]
   [0 0 1]])

(def l-tetromino
  [[1 1 1]
   [1 0 0]])

(def o-tetromino
  [[1 1]
   [1 1]])

(def s-tetromino
  [[0 1 1]
   [1 1 0]])

(def t-tetromino
  [[0 1 0]
   [1 1 1]
   [0 0 0]])

(def z-tetromino
  [[1 1 0]
   [0 1 1]])

(defn rotate-grid
  "Rotate 2d grid, intended for tetrominos"
  [grid]
  (vec (apply map vector (reverse grid))))

(def all-tetrominos
  [i-tetromino
   j-tetromino
   l-tetromino
   o-tetromino
   s-tetromino
   t-tetromino
   z-tetromino])

(defn random-tetromino
  []
  (nth all-tetrominos (rand-int (count all-tetrominos))))

(def field-width 10)
(def field-height 22)
(def start-position [0 3])

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

(defn grid-string
  [grid]
  (clojure.string/join "\n" (map #(apply str %) grid)))

(defn print-grid
  "Print a grid as one row per line with no spaces"
  [grid]
  (.log js/console (grid-string grid))
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

(defn horizontal-out-of-bounds?
  "True if :position puts all or part of :piece outside of :grid"
  [{:keys [grid piece position] :as game}]
  (let [grid-width (count (first grid))
        flat-piece (apply map + piece)
        x-offset (count (take-while zero? flat-piece))
        inner-width (count (filter (complement zero?) flat-piece))
        offset-position (+ (second position) x-offset)]
    (or (< grid-width (+ offset-position inner-width))
        (< offset-position 0))))

(defn vertical-out-of-bounds?
  "True of :position puts all or part of :piece beyond bottom of :grid"
  [{:keys [grid piece position] :as game}]
  (let [grid-height (count grid)
        flat-piece (map (partial apply +) piece)
        y-offset (count (take-while zero? flat-piece))
        inner-height (count (filter (complement zero?) flat-piece))
        offset-position (+ (first position) y-offset)]
    (< grid-height (+ offset-position inner-height))))

(defn lock-piece
  [{:keys [grid piece position] :as game}]
  (assoc game :grid (merge-grid grid piece position)))

(defn row-full?
  [row]
  (every? (partial < 0) row))

(defn count-full-rows
  "Returns number of full rows on grid"
  [grid]
  (count (filter row-full? grid)))

(defn game-score-rows
  [game]
  (update-in game [:lines] (partial + (count-full-rows (:grid game)))))

(defn clear-rows
  "Remove full rows from the field"
  [grid]
  (let [cols (count (first grid))
        n-full-rows (count-full-rows grid)
        new-rows (take n-full-rows (repeatedly #(empty-row cols)))
        cleared-grid (filterv (complement row-full?) grid)]
    (vec (concat new-rows cleared-grid))))

(defn game-clear-rows
  [{:keys [grid] :as game}]
  (assoc game :grid (clear-rows grid)))

(defn activate-next
  [{:keys [piece next] :as game}]
  (-> game
      (assoc :piece next)
      (assoc :next (random-tetromino))
      (assoc :position start-position)))

(defn n-rows-dirty-grid
  [n]
  (vec (concat
        (make-grid 0 field-width (- field-height n))
        (random-grid field-width n))))

(defn new-game
  []
  {:grid (n-rows-dirty-grid 3)
   :position start-position
   :piece (random-tetromino)
   :next (random-tetromino)
   :lines 0})

(defn hit?
  "Current game has a piece that overlaps a block or is
  outside of the boundaries of the grid"
  [game]
  (or (overlapping? game)
      (horizontal-out-of-bounds? game)
      (vertical-out-of-bounds? game)))

(defn move-clockwise
  [game]
  (let [moved (update-in game [:piece] rotate-grid)]
    (if (or (overlapping? moved)
            (horizontal-out-of-bounds? moved))
      game
      moved)))

(defn move-right
  [game]
  (let [moved (update-in game [:position 1] inc)]
    (if (or (overlapping? moved)
            (horizontal-out-of-bounds? moved))
      game
      moved)))

(defn move-left
  [game]
  (let [moved (update-in game [:position 1] dec)]
    (if (or (overlapping? moved)
            (horizontal-out-of-bounds? moved))
      game
      moved)))

(defn move-down
  [game]
  (let [moved (update-in game [:position 0] inc)]
    (if (or (overlapping? moved)
            (vertical-out-of-bounds? moved))
      (-> game
          lock-piece
          game-score-rows
          game-clear-rows
          activate-next)
      moved)))

(defn move-drop
  [game]
  (let [max-tries (count (:grid game))
        down #(update-in % [:position 0] inc)
        moves (take max-tries (iterate down game))
        move-pairs (map vector moves (drop 1 moves))]
    (if-let [move (some #(when (hit? (second %)) %) move-pairs)]
      (move-down (first move))
      game)))

(defn step-game
  "Advance game one frame"
  [game input]

  (case input
    :up (move-clockwise game)
    :left (move-left game)
    :right (move-right game)
    :down (move-down game)
    :drop (move-drop game)
    game))
