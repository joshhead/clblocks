(ns cltetris.runner
  (:require [cltetris.game :as cltetris]
            [cltetris.platform :as platform]
            #+clj [clojure.core.async :as async :refer [go]]
            #+cljs [cljs.core.async :as async])
  #+cljs (:require-macros [cljs.core.async.macros :refer [go]]))

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
        keysc (platform/setup-key-listener frame)
        ticker (tick-chan 500)
        closec (platform/setup-close-listener frame)
        quitc (async/chan)]

    (go (loop [game (cltetris/new-game)]
                (let [[val port] (async/alts! [keysc ticker])
                      key (if (= port keysc) val [:down :press])]
                  (when (= (key 1) :press)
                    (let [next-game (cltetris/step-game game (key 0))]
                      (platform/frame-draw-game frame next-game)))
                  (when-not (or (nil? key) (= (key 0) :escape))
                    (if (= (key 1) :press)
                      (recur (cltetris/step-game game (key 0)))
                      (recur game)))))
              (async/>! closec :quit))

    ; Close window if using java.awt
    #+clj
    (go
     (async/<! closec)
     (.hide frame)
     (async/close! quitc))

    quitc))

#+clj
(defn -main
  [& args]
  (async/<!! (play))
  (System/exit 0))

#+cljs
(defn ^:export main
  []
  (play))

(comment
  (play)
)

