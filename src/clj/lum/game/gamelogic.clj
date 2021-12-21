(ns lum.game.gamelogic
  (:require
   [lum.game.cavegen :as cavegen]
   [lum.maputil :as mu]
   [lum.game.dataspec]
   [clojure.string]
   [clojure.spec.alpha :as s]
   [clojure.core.async :as a :refer [chan go go-loop <! >! close!]]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]))



(defn set-position
  [data [_ x y]]
  (assoc-in data [:player :position] [x y]))

(defn move
  [data [_ direction]]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc))
        position (get-in new-data [:player :position])]
    (if (and (s/valid? :game/position position)
             (= :ground
                (:type (mu/get-tile (:board new-data)
                                    (get-in new-data [:player :position 0])
                                    (get-in new-data [:player :position 1])))))
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



(defn new-board
  [data _]
  (assoc data :board (cavegen/get-dungeon)))


(defn initialize
  [_ _]
    {:board (cavegen/get-dungeon)
     :npcs []
     :player {:position [10 10]
              :xp 0
              :hp [10 10]
              :mp [3 3]}
     :fight? false})

(defn load-map
  [data [_ file]]
  (if-let [mf (try
                (slurp (io/resource file))
                (catch Exception e (log/error "Exception thrown " (.getMessage e))))]
    (assoc data :board (load-map-from-string mf))
    data))

(defn check-fight
  [data _]
  (if (> (rand) 0.97)
    ;;Start a fight every 20 turns
    (assoc data :fight {:enemy {:name "Bat"
                                :hp [2 2]
                                :mp [0 0]}
                        :actions []})
    data))

(defn attack
  [data _]
  (dissoc data :fight))

(def calc-new-state-functions
  {:initialize [initialize]
   :load-map [load-map]
   :move [move check-fight]
   :set-position [set-position]
   :new-board [new-board]
   :attack [attack]
   :nop []})


(defn enforce-spec
  [f]
  (fn [data command]
    (let [new-data (f data command)]
      (if (s/valid? :game/game new-data)
        new-data
        (do
          (log/error (s/explain-str :game/game new-data))
          data)))))

(defn calc-new-state
  [data action]
  (if action
    (do
      (when (nil? (get calc-new-state-functions (keyword (first action))))
        (log/error "No entry defined " action))
      (reduce (fn [data f]
                ((enforce-spec f) data action))
              data (get calc-new-state-functions
                        (keyword (first action)) [])))
    data))


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

(defn fight
  [data new-data]
  (when (not= (:fight data)
              (:fight new-data))
    [:fight (:fight new-data)]))

(defn hp-update
  [data new-data]
  (when (not= (-> data :player :hp)
              (-> new-data :player :hp))
    (let [[current max] (-> new-data :player :hp)]
      [:hp current max])))

(defn mp-update
  [data new-data]
  (when (not= (-> data :player :mp)
              (-> new-data :player :mp))
    (let [[current max] (-> new-data :player :mp)]
      [:mp current max])))

(defn xp-update
  [data new-data]
  (when (not= (-> data :player :xp)
              (-> new-data :player :xp))
    [:xp (-> new-data :player :xp)]))

(def update-calc-functions
  [board-update
   player-move
   fight
   hp-update
   mp-update
   xp-update])


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

