(ns clblocks.components
  (:require [clblocks.game :as clblocks]
            [clojure.string :as clojure.string]
            [cljs.core.async :as async]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn om-cell
  [cell]
  (let [class-name (condp = (last cell)
                     nil "clblocks__cell--empty"
                     :i "clblocks__cell--i"
                     :j "clblocks__cell--j"
                     :l "clblocks__cell--l"
                     :o "clblocks__cell--o"
                     :s "clblocks__cell--s"
                     :t "clblocks__cell--t"
                     :z "clblocks__cell--z"
                     "clblocks__cell--full")]
       (dom/div #js {:className class-name})))

(defn om-row
  [row]
  (dom/div
   #js {:className "row"}
   (into-array
    (concat
     (map om-cell row)))))

(defn om-grid
  [grid]
  (dom/div
   #js {:className "clblocks__grid"}
   (into-array (concat (map om-row grid)))))

(defn om-next
  [{:keys [next]}]
  (dom/div
   #js {:className "clblocks__status__next"}
   (array "Next: " (om-grid next))))

(defn om-score
  [{:keys [lines count]}]
  (dom/div
   #js {:className "clblocks__status__score"}
   (dom/div
    #js {}
    (str "Level: " (js/Math.floor (/ lines 10))))
   (dom/div
    #js {}
    (str "Lines: " lines))
   (dom/div
    #js {}
    (str "Piece: " count))))

(defn game-view
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div
       nil
       (let [game (:game data)]
         (array
          (dom/div
           #js {:className "clblocks__field"}
           (om-grid (clblocks/game-merged-grid game)))
          (dom/div
           #js {:className "clblocks__status"}
           (om-next game)
           (om-score game))
          (when (clblocks/game-over? game)
            (dom/div
             #js {:className "clblocks__text-overlay"}
             "GAME OVER"))
          (when (:paused? game)
            (dom/div
             #js {:className "clblocks__text-overlay"}
             "PAUSED"))))))))

