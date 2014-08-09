(ns clblocks.runner
  (:require [clblocks.game :as clblocks]
            [clblocks.components :as components]
            [clblocks.events :as events]
            [cljs.core.async :as async]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn- now
  []
  (.valueOf (new js/Date)))

(defn tick-chan
  [ms]
  (let [ticker (async/chan)
        control (async/chan)]
    (go-loop [start (now)
              timeout ms]
      (let [[val port] (async/alts! [control (async/timeout timeout)])]
        (if (= val :pause)
          (let [elapsed (- (now) start)
                diff (- ms elapsed)
                remaining (if (pos? diff) diff ms)]
            (async/<! control)
            (recur (now) remaining))
          (do
            (async/>! ticker :tick)
            (recur (now) ms)))))
    [ticker control]))

(defn play
  []
  (let [app-state (atom {:game (clblocks/new-game)})
        [ticker tick-control] (tick-chan 500)
        ; Deal only with key presses
        keysc (async/map< first (async/filter< #(= (second %) :press) (events/setup-key-listener js/document)))]

    (go
      (loop [game (:game @app-state)]
        (let [[val port] (async/alts! [keysc ticker])
              key (if (= port keysc) val :down)
              next-game (clblocks/step-game game key)]

          (swap! app-state #(assoc % :game next-game))

          (when-not (or (nil? key) (clblocks/game-over? next-game))
            (recur next-game))))

      ; Unpause game after it ends, game-over+paused is redundant
      (swap! app-state #(update-in % [:game] clblocks/unpause)))

    app-state))

(defn ^:export main
  []
  (let [app-state (play)]
    (set! (.-app-state js/window) app-state)
    (om/root
     components/game-view
     app-state
     {:target (.getElementById js/document "clblocks")})))
