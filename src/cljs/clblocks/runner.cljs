(ns clblocks.runner
  (:require [clblocks.game :as clblocks]
            [clblocks.components :as components]
            [clblocks.events :as events]
            [cljs.core.async :as async]
            [om.core :as om :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

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
  "Return a vector of two channels [ticker control].
  Reads on ticker will return tick every n milliseconds
  where n is the argument supplied to this function.
  Writing :pause to the control channel will cause ticker
  to park until value :unpause is placed on control.

  It is safe to put :pause or :unpause twice in a row,
  values other than the expected :pause or :unpause will
  be ignored."
  [ms]
  (let [ticker (async/chan)
        control (async/chan)]
    (go-loop [start (now)
              timeout (async/timeout ms)]
      (let [[val port] (async/alts! [control timeout])]
        (cond
         ; non :pause value on control channel, ignore
         (and (= port control) (not= val :pause))
         (recur start timeout)

         ; got :pause on control channel, wait for :unpause
         (and (= port control) (= val :pause))
         (let [elapsed (- (now) start)
                  diff (- ms elapsed)
                  remaining (if (pos? diff) diff ms)]
           (if @show-timing?
             (.log js/console (- (now) start)))
              (loop []
                (when (not= (async/<! control) :unpause)
                  (recur)))
              (recur (now) (async/timeout remaining)))

         ; timeout, put :tick and start over
         :else
         (do
           (async/>! ticker :tick)
           (if @show-timing?
             (.log js/console (- (now) start)))
           (recur (now) (async/timeout ms))))))
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

          ; Trigger om rerender
          (swap! app-state #(assoc % :game next-game))

          ; Pause/unpause ticker to match game
          (if (clblocks/paused? next-game)
            (async/put! tick-control :pause)
            (async/put! tick-control :unpause))

          (when-not (or (nil? key) (clblocks/game-over? next-game))
            (recur next-game))))

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
