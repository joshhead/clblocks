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
    (go
     (loop []
       (async/<! (async/timeout ms))
       (async/>! tickc :tick)
       (recur)))
    tickc))

(defn play
  []
  (let [app-state (atom {:game (clblocks/new-game) :play-state {:paused false :game-over false}})
        ticker (tick-chan 500)
        ; Deal only with key presses
        keysc (async/map< first (async/filter< #(= (second %) :press) (events/setup-key-listener js/document)))]

    (go
      (loop [game (:game @app-state)]
        (let [[val port] (async/alts! [keysc ticker])
              key (if (= port keysc) val :down)
              next-game (if (get-in @app-state [:play-state :paused])
                          game
                          (clblocks/step-game game key))]

          (swap! app-state (fn [state]
                             (-> state
                                 (assoc :game next-game)
                                 (update-in [:play-state :paused] (if (= key :pause) not identity)))))

          (when-not (or (nil? key) (= key :escape) (clblocks/game-over? next-game))
            (recur next-game))))

      (swap! app-state (fn [state]
                         (-> state
                             (assoc-in [:play-state :game-over] true)
                             (assoc-in [:play-state :paused] false)))))

    app-state))

(defn ^:export main
  []
  (let [app-state (play)]
    (set! (.-app-state js/window) app-state)
    (set! (.-pause-unpause-game js/window) (fn [] (swap! app-state #(update-in % [:play-state :paused] not))))
    (om/root
     components/game-view
     app-state
     {:target (.getElementById js/document "clblocks")})))
