(ns lum.routes.websockets
  (:require
   [org.httpkit.server :refer [send! as-channel]]
   [cheshire.core :as json]
   [clojure.walk]
   [lum.game.cavegen :as cavegen]
   [clojure.core.async :as a :refer [chan go go-loop <! >! close!]]
   [clojure.tools.logging :as log]
   [lum.game.cavegen :as g]
   [lum.maputil :as mu]
   [clojure.string]
   [clojure.spec.alpha :as s]
   [clojure.java.io :as io]))

(defmulti calc-new-state
  (fn [_ action]
    (log/info "calc-new-state" action (keyword (first action)))
    (keyword (first action))))

(defmethod calc-new-state
  :set-position
  [data [_ x y]]
  (assoc-in data [:player :position] [x y]))

(defmethod calc-new-state
  :move
  [data [_ direction]]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc))
        position (get-in new-data [:player :position])]
    (if (s/valid? :game/position position)
      new-data
      data)))

(defn pad [n pad coll]
  (take n (concat coll (repeat pad))))

(defn load-map-from-string
  [inp]
  (->> (clojure.string/split-lines inp)
       (map (fn [line]
              (->> (seq line)
                   (map (fn [c]
                          (case c
                            \  {:type :ground}
                            \. {:type :ground}
                            \# {:type :wall}
                            {:type :wall}))))))
       (map (fn [line]
              (pad mu/sizex {:type :wall} line)))
       flatten
       (pad (* mu/sizex mu/sizey) {:type :wall})))


(defmethod calc-new-state
  :load-map
  [data [_ file]]
  (if-let [mf (try
                (slurp (io/resource file))
                (catch Exception e (log/error "Exception thrown " (.getMessage e))))]
    (assoc data :board (load-map-from-string mf))
    data))

(defmethod calc-new-state
  :default
  [data action]
  (log/error "Default reached " action)
  data)

(defmethod calc-new-state
  :new-board
  [data _]
  (assoc data :board (g/get-dungeon)))

(defmethod calc-new-state
  :initialize
  [_ _]
  {:board (g/get-dungeon)
   :npcs []
   :player {:position [10 10]}})

(defn board-update
  [data new-data]
  (if (not= (:board data)
            (:board new-data))
    [:new-board (:board new-data)]
    nil))

(defn player-move
  [data new-data]
  (when (not= (get-in data [:player :position])
              (get-in new-data [:player :position]))
    (let [[x y] (get-in new-data [:player :position])]
      [:player-move x y])))

(def update-calc-functions
  [board-update
   player-move])

(defn calc-updates [data new-data]
  (filter (complement nil?)
          (reduce (fn [r f]
                    ;;(log/info (f data new-data))
                    (conj r (f data new-data)))
                  []
                  update-calc-functions)))

(defn game-master
  [input-chan]
  (let [out (chan)]
    (go-loop [data {}]
      (let [action (<! input-chan)
            new-data (calc-new-state data action)
            updates (calc-updates data new-data)]
        (if (some? action)
          (do
            (doseq [update updates] (>! out update))
            (recur new-data))
          (do
            (close! out)
            data))))
    out))

(defn connect!
  [in out]
  (fn [chan]
    (log/info "Channel open")
    (go (>! in [:initialize]))
    (go-loop []
      (when-let [msg (<! out)]
        (send! chan (json/generate-string msg))
        (recur)))))

(defn disconnect!
  [_ status]
  (log/info "Channel closed" status))

(defmulti dispatch-ws
  (fn [_ msg]
    (keyword (:type msg))))

(defmethod dispatch-ws
  :player-move
  [_ msg]
  (log/info "Player move" (:direction msg)))

(defmethod dispatch-ws
  :new-board
  [c _]
  (log/info "new board")
  (send! c (json/generate-string {:type :set-board
                                  :board (cavegen/get-dungeon)})))

(defmethod dispatch-ws
  :default
  [_ msg]
  (log/info "default dispatch" msg))

(defn handle-receive!
  [in]
  (fn
    [_ message]
    (log/info message)
    (let [message (json/parse-string-strict message)]
      (go (>! in message)))))

(defn ws-handler [request]
  (if-not (:websocket? request)
    {:status 200 :body "Game channel"}
    (let [in (chan)
          out (game-master in)]
      (as-channel request
                  {:on-open (connect! in out)
                   :on-close disconnect!
                   :on-receive (handle-receive! in)}))))

(def ws-routes
  ["/game/ws" ws-handler])
