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

(defn drop-delay
  [{:keys [lines] :as game}]
  (let [level (js/Math.floor (/ lines 10))]
    (- 500 (* level 45))))

(defn get-touch-info
  [touch]
  {:identifier (.-identifier touch)
   :screen-x (.-screenX touch)
   :screen-y (.-screenY touch)})

(defn touch-delta->action
  [start end]
  (let [horizontal (- (:screen-x end) (:screen-x start))
        vertical (- (:screen-y end) (:screen-y start))]
    (if (> (js/Math.abs horizontal)
           (js/Math.abs vertical))
      (if (pos? horizontal)
        :right
        :left)
      (if (pos? vertical)
        :drop
        :up))))

(defn handle-start
  [active-touch-info]
  (fn [e]
    (.preventDefault e)
    (let [touches (.-touches e)]
      (when (= 1 (.-length touches) 1)
        (reset! active-touch-info (get-touch-info (aget touches 0)))))))

(defn handle-end
  [active-touch-info action-chan]
  (fn [e]
    (.preventDefault e)
    (let [touches (.-changedTouches e)
          active-info @active-touch-info]
      (when active-info
        (loop [i 0]
          (let [touch (aget touches i)]
            (if (= (.-identifier touch)
                   (:identifier active-info))
              (let [action (touch-delta->action active-info
                                                (get-touch-info touch))]
                (async/put! action-chan action)
                (reset! active-touch-info nil))
              (when (< i (.-length touches))
                (recur (inc i))))))))))

(defn play
  []
  (let [app-state (atom {:game (clblocks/new-game)})
        tick-control (async/chan)
        active-touch-info (atom nil)
        touchc (async/chan)
        ; Deal only with key presses
        keysc (async/map< first (async/filter< #(= (second %) :press) (events/setup-key-listener js/document)))]

    (.addEventListener (js/document.getElementById "clblocks")
                       "touchstart"
                       (handle-start active-touch-info)
                       false)
    (.addEventListener (js/document.getElementById "clblocks")
                       "touchend"
                       (handle-end active-touch-info touchc)
                       false)

    (go
      (loop [game (:game @app-state)
             ticker (tick-chan (drop-delay game) tick-control)]
        (let [[val port] (async/alts! [keysc touchc ticker])
              key (if (or (= port keysc) (= port touchc))
                    val
                    :down)
              next-game (clblocks/step-game game key)
              next-ticker (cond
                           (= port ticker)
                           (tick-chan (drop-delay next-game) tick-control)

                           (< (:count game) (:count next-game))
                           (tick-chan (drop-delay next-game) tick-control)

                           :else
                           ticker)]

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
