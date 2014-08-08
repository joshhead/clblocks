(ns clblocks.events
  (:require [cljs.core.async :as async]
            [goog.events :as events]
            [goog.events.KeyCodes]))


(def keycodes {goog.events.KeyCodes.UP :up
               goog.events.KeyCodes.DOWN :down
               goog.events.KeyCodes.LEFT :left
               goog.events.KeyCodes.RIGHT :right
               goog.events.KeyCodes.SPACE :drop
               goog.events.KeyCodes.ESC :pause
               goog.events.KeyCodes.P :pause})

(defn get-event-keyword
  [e]
  (get keycodes (.-keyCode e) nil))

(defn setup-key-listener
  [element]
  (let [event-chan (async/chan)]

    (goog.events.listen
     element
     goog.events.EventType.KEYDOWN
     (fn [e]
       (when-let [event-kw (get-event-keyword e)]
         (async/put! event-chan [event-kw :press]))))

    (goog.events.listen
     element
     goog.events.EventType.KEYUP
     (fn [e]
       (when-let [event-kw (get-event-keyword e)]
         (async/put! event-chan [event-kw :release]))))

    event-chan))
