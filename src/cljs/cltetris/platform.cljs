(ns cltetris.platform
  (:require [cltetris.game :as cltetris]
            [clojure.string :as clojure.string]
            [cljs.core.async :as async]
            [goog.dom :as gdom]
            [goog.events :as events]
            [goog.events.KeyCodes]
            [om.core :as om]
            [om.dom :as dom :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def keycodes {goog.events.KeyCodes.UP :up
               goog.events.KeyCodes.DOWN :down
               goog.events.KeyCodes.LEFT :left
               goog.events.KeyCodes.RIGHT :right
               goog.events.KeyCodes.SPACE :drop
               goog.events.KeyCodes.ESC :escape})

(def game-states (async/chan))

(declare setup-game-om-root)

(defn new-frame
  ([]
     (new-frame 200 440))
  ([width height]
     (setup-game-om-root "om" game-states)
     (gdom/getElement "cltetris")))

(defn get-event-keyword
  [e]
  (get keycodes (.-keyCode e) nil))

(defn setup-key-listener
  [frame]
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

(defn ^:private cell-markup
  [cell]
  (if (< 0 cell)
    "<div class='cltetris__cell--full'></div>"
    "<div class='cltetris__cell--empty'></div>"))

(defn ^:private row-markup
  [row]
  (apply str (concat ["<div class='row'>"] (map cell-markup row) ["</div>"])))

(defn ^:private grid-markup
  [grid]
  (apply str (concat ["<div class='cltetris__grid'>"] (map row-markup grid) ["</div>"])))

(defn ^:private score-markup
  [{:keys [lines]}]
  (str "<div class='cltetris__status__score'>Lines: " lines "</div>"))

(defn ^:private next-markup
  [{:keys [next]}]
  (str "<div class='cltetris__status__next'>Next:" (grid-markup next) "</div>"))

(defn ^:private status-markup
  [game]
  (str "<div class='cltetris__status'>" (next-markup game) (score-markup game) "</div>"))

(defn game-merged-grid
  "Take a game and return the grid with the current piece merged in"
  [{:keys [grid piece position] :as game}]
  (cltetris/merge-grid grid piece position))

(defn ^:private game-markup
  [game]
  (str "<div class='cltetris__field'>" (grid-markup (game-merged-grid game)) "</div>"
       (status-markup game)))

(defn frame-draw-game
  [frame game]
  (async/put! game-states game)
  (set! (.-innerHTML frame) (game-markup game)))

(defn setup-close-listener
  [frame]
  (async/chan))

(defn om-cell
  [cell]
  (if (> cell 0)
    (dom/div #js {:className "cltetris__cell--full"})
    (dom/div #js {:className "cltetris__cell--empty"})))

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
   #js {:className "cltetris__grid"}
   (into-array (concat (map om-row grid)))))

(defn om-next
  [{:keys [next]}]
  (dom/div
   #js {:className "cltetris__status__next"}
   (array "Next: " (om-grid next))))

(defn om-score
  [{:keys [lines]}]
  (dom/div
   #js {:className "cltetris__status__score"}
   (str "Lines: " lines)))

(defn setup-game-om-root
  "id: the id of an element to mount the component at
  game-states: a channel. Put cltetris game states on it"
  [id game-states]
    (om/root
     {:game nil}
     (fn [data]
       (reify
         dom/IWillMount
         (-will-mount [_ _]
           (go (while true
                 (om/replace! data [:game] (async/<! game-states)))))
         dom/IRender
         (-render [_ _]
           (dom/div
            nil
            (when-let [game (:game data)]
              (array
               (dom/div
                #js {:className "cltetris__field"}
                (om-grid (game-merged-grid (:game data))))
               (dom/div
                #js {:className "cltetris__status"}
                (array
                 (om-next (:game data))
                 (om-score (:game data))))))))))
     (gdom/getElement id)))
