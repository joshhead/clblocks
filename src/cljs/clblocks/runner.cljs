(ns clblocks.runner
  (:require [clblocks.game :as clblocks]
            [clblocks.components :as components]
            [clblocks.events :as events]
            [cljs.core.async :as async]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def show-timing? (atom false))

(defn ^:export show-timing
  [show?]
  (if show?
    (reset! show-timing? true)
    (reset! show-timing? false)))

(defn- now
  []
  (.valueOf (new js/Date)))

(defn tick-chan
  [ms control]
  (let [ticker (async/chan)]
    (go-loop [start (now)
              timeout (async/timeout ms)]
      (let [[val port] (async/alts! [control timeout])]
        (cond
         ; already unpaused, ignore :unpause
         (and (= port control) (= val :unpause))
         (recur start timeout)

         ; got :pause on control channel, wait for :unpause
         (and (= port control) (= val :pause))
         (let [elapsed (- (now) start)
                  diff (- ms elapsed)
                  remaining (max diff 0)]
           (if @show-timing?
             (pr (- (now) start)))
              (loop []
                (when (not= (async/<! control) :unpause)
                  (recur)))
              (recur (now) (async/timeout remaining)))

         ; timeout, close ticker
         :else
         (do
           (if @show-timing?
             (pr (- (now) start)))
           (async/close! ticker)))))
    ticker))

(defn play
  []
  (let [app-state (atom {:game (clblocks/new-game)})
        tick-control (async/chan)
        ticker (tick-chan 500 tick-control)
        ; Deal only with key presses
        keysc (async/map< first (async/filter< #(= (second %) :press) (events/setup-key-listener js/document)))]

    (go
      (loop [game (:game @app-state)
             ticker ticker]
        (let [[val port] (async/alts! [keysc ticker])
              key (if (= port keysc) val :down)
              next-game (clblocks/step-game game key)
              next-ticker (cond
                           (= port ticker) (tick-chan 500 tick-control)
                           (< (:count game) (:count next-game)) (tick-chan 500 tick-control)
                           :else ticker)]

          ; Trigger om rerender
          (swap! app-state #(assoc % :game next-game))

          ; Pause/unpause ticker to match game
          (if (clblocks/paused? next-game)
            (async/put! tick-control :pause)
            (async/put! tick-control :unpause))

          (when-not (or (nil? key) (clblocks/game-over? next-game))
            (recur next-game next-ticker))))

      ; Unpause game after it ends. Pause after game-over is unnecessary
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
