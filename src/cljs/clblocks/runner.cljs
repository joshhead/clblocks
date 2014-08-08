(ns clblocks.runner
  (:require [clblocks.game :as clblocks]
            [clblocks.components :as components]
            [clblocks.events :as events]
            [cljs.core.async :as async]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn tick-chan
  [ms]
  (let [tickc (async/chan)]
    (go-loop []
      (async/<! (async/timeout ms))
      (async/>! tickc :tick)
      (recur))
    tickc))

(defn play
  []
  (let [app-state (atom {:game (clblocks/unpause (clblocks/new-game))})
        ticker (tick-chan 500)
        ; Deal only with key presses
        keysc (async/map< first (async/filter< #(= (second %) :press) (events/setup-key-listener js/document)))]

    (go
      (loop [game (:game @app-state)]
        (let [[val port] (async/alts! [keysc ticker])
              key (if (= port keysc) val :down)
              next-game (if (and
                             (get-in @app-state [:game :paused?])
                             (not= key :pause))
                          game
                          (clblocks/step-game game key))]

          (swap! app-state #(assoc % :game next-game))

          (when-not (or (nil? key) (clblocks/game-over? next-game))
            (recur next-game))))

      (swap! app-state #(assoc-in % [:game :paused?] false)))

    app-state))

(defn ^:export main
  []
  (let [app-state (play)]
    (set! (.-app-state js/window) app-state)
    (om/root
     components/game-view
     app-state
     {:target (.getElementById js/document "clblocks")})))
