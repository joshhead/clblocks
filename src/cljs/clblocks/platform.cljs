(ns clblocks.platform
  (:require [clblocks.game :as clblocks]
            [clojure.string :as clojure.string]
            [cljs.core.async :as async]
            [goog.events :as events]
            [goog.events.KeyCodes]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def keycodes {goog.events.KeyCodes.UP :up
               goog.events.KeyCodes.DOWN :down
               goog.events.KeyCodes.LEFT :left
               goog.events.KeyCodes.RIGHT :right
               goog.events.KeyCodes.SPACE :drop
               goog.events.KeyCodes.ESC :escape})

(defn get-event-keyword
  [e]
  (get keycodes (.-keyCode e) nil))

(defn setup-key-listener
  []
  (let [event-chan (async/chan)]

    (goog.events.listen
     js/document
     goog.events.EventType.KEYDOWN
     (fn [e]
       (when-let [event-kw (get-event-keyword e)]
         (async/put! event-chan [event-kw :press]))))

    (goog.events.listen
     js/document
     goog.events.EventType.KEYUP
     (fn [e]
       (when-let [event-kw (get-event-keyword e)]
         (async/put! event-chan [event-kw :release]))))

    event-chan))

(defn game-merged-grid
  "Take a game and return the grid with the current piece merged in"
  [{:keys [grid piece position] :as game}]
  (clblocks/merge-grid grid piece position))

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
  [game-states]
  (fn [data owner]
    (reify
      om/IRender
      (render [_]
        (dom/div
         nil
         (when-let [game (:game data)]
           (array
            (dom/div
             #js {:className "clblocks__field"}
             (om-grid (drop 2 (game-merged-grid game))))
            (dom/div
             #js {:className "clblocks__status"}
             (array
              (om-next game)
              (om-score game)))
            (when (clblocks/game-over? game)
              (dom/div
               #js {:className "clblocks__text-overlay"}
               "GAME OVER")))))))))

(defn setup-game-om-root
  "id: the id of an element to mount the component at
  game-states: a channel. Put clblocks game states on it"
  [id game-states app-state]
  (om/root
   (game-view game-states)
   app-state
   {:target (.getElementById js/document id)}))
