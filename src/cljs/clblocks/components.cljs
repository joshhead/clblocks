(ns clblocks.components
  (:require [clblocks.game :as clblocks]
            [clojure.string :as clojure.string]
            [cljs.core.async :as async]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn om-cell
  [cell]
  (if (> cell 0)
    (dom/div #js {:className "clblocks__cell--full"})
    (dom/div #js {:className "clblocks__cell--empty"})))

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
  [{:keys [lines]}]
  (dom/div
   #js {:className "clblocks__status__score"}
   (str "Lines: " lines)))

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

