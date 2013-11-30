(ns cltetris.tetrominos)

(comment
  [[0 0 0 0]
   [0 0 0 0]
   [0 0 0 0]
   [0 0 0 0]])

(def i
  [[0 0 1 0]
   [0 0 1 0]
   [0 0 1 0]
   [0 0 1 0]])

(def o
  [[1 1]
   [1 1]])

(def t
  [[0 1 0]
   [1 1 1]
   [0 0 0]])

(defn rotate
  [tetromino]
  (vec (apply map vector (reverse tetromino))))

(def all
  [i o t])

(defn random
  []
  (nth all (rand-int (count all))))