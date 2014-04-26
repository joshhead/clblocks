(ns cltetris.runner
  (:require [cltetris.game :as cltetris]
            [cltetris.platform :as platform]
            [cljs.core.async :as async])
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

(def app-state (atom {:game (cltetris/new-game)}))
(def game-states (async/chan))

(platform/setup-game-om-root "cltetris" game-states app-state)

(defn play
  []
  (let [
        ; Deal only with key presses
        keysc (async/map< first (async/filter< #(= (second %) :press) (platform/setup-key-listener)))
        ticker (tick-chan 500)]

    (go-loop [game (:game @app-state)]
        (let [[val port] (async/alts! [keysc ticker])
              key (if (= port keysc) val :down)
              next-game (cltetris/step-game game key)]
          (swap! app-state (constantly {:game next-game}))
          (when-not (or (nil? key) (= key :escape) (cltetris/game-over? next-game))
            (recur next-game))))))

(defn ^:export main
  []
  (play))

(comment
  (play)
)

