(ns cltetris.platform
  (:require [cltetris.tetrominos :as tetrominos]
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

(defn frame-draw-grid
  [frame grid]
  (let [el-grid (mapv (fn [row] (mapv #(if (< 0 %) "<div class='cltetris__cell--full'></div>" "<div class='cltetris__cell--empty'></div>") row)) grid)
        grid-html (apply str (map #(str "<div class='row'>" % "</div>") (map #(apply str %) el-grid)))]
    (set! (.-innerHTML frame) grid-html)))

(defn setup-close-listener
  [frame]
  (async/chan))
