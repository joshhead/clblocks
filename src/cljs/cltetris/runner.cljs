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

(defn play
  []
  (let [frame (platform/new-frame 200 440)
        ; Deal only with key presses
        keysc (async/map< first (async/filter< #(= (second %) :press) (platform/setup-key-listener frame)))
        ticker (tick-chan 500)
        closec (platform/setup-close-listener frame)
        quitc (async/chan)
        initial-game (cltetris/new-game)]

    (platform/frame-draw-game frame initial-game)

    (go-loop [game initial-game]
        (let [[val port] (async/alts! [keysc ticker])
              key (if (= port keysc) val :down)
              next-game (cltetris/step-game game key)]
          (platform/frame-draw-game frame next-game)
          (when-not (or (nil? key) (= key :escape))
            (recur next-game)))
        (async/>! closec :quit))

    quitc))

(defn ^:export main
  []
  (play))

(comment
  (play)
)

