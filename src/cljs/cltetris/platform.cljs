(ns cltetris.platform
  (:require [cltetris.game :as cltetris]
            [clojure.string :as clojure.string]
            [cljs.core.async :as async]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.events.KeyCodes])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def keycodes {goog.events.KeyCodes.UP :up
               goog.events.KeyCodes.DOWN :down
               goog.events.KeyCodes.LEFT :left
               goog.events.KeyCodes.RIGHT :right
               goog.events.KeyCodes.SPACE :drop
               goog.events.KeyCodes.ESC :escape})

(defn new-frame
  ([]
     (new-frame 200 440))
  ([width height]
     (dom/getElement "cltetris")))

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

(defn ^:private game-markup
  [{:keys [grid piece position] :as game}]
  (let [drawable-grid (cltetris/merge-grid grid piece position)]
    (str "<div class='cltetris__field'>" (grid-markup drawable-grid) "</div>"
         (status-markup game))))

(defn frame-draw-game
  [frame game]
  (set! (.-innerHTML frame) (game-markup game)))

(defn setup-close-listener
  [frame]
  (async/chan))
