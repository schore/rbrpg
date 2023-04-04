(ns lum.game.utilities
  (:require [lum.maputil :as mu]
            [lum.game.game-database :as db]
            [lum.game.board :as board]))

(defn add-message
  [data message]
  (update data :messages #(take 10 (conj % message))))

(defn fight-ended?
  [data]
  (= 0 (get-in data [:fight :enemy :hp 0])))

(defn game-over?
  [data]
  (= 0 (get-in data [:player :hp 0])))

(defn filter-map
  [f m]
  (into {} (filter f m)))

(defn map-map
  [f m]
  (into {} (map f m)))

(defn get-target-keys
  [target & ext]
  (concat (case target
            :player [:player]
            :enemy [:fight :enemy])
          ext))

(defn add-with-boundaries
  [min max & vals]
  (let [s (reduce + vals)]
    (cond
      (< s min) min
      (> s max) max
      :else s)))

(defn add-hp
  [state target n]
  (update-in state (get-target-keys target :hp)
             (fn [[v max]] [(add-with-boundaries 0 max v n) max])))

(defn add-mp
  [state target n]
  (update-in state (get-target-keys target :mp)
             (fn [[v max]] [(add-with-boundaries 0 max v n) max])))

(defn add-max-hp
  [state target n]
  (update-in state (get-target-keys target :hp)
             (fn [[v max]] [(+ v n) (+ max n)])))

(defn process-stat
  [k f]
  (fn [state action-name effect]
    (if (contains? effect k)
      (-> state
          (add-message (str action-name ": " (get effect k) (str k)))
          (f (:target effect) (get effect k)))
      state)))

(def process-hp (process-stat :hp add-hp))

(def process-mp (process-stat :mp add-mp))

(def process-max-hp (process-stat :maxhp add-max-hp))

(defn process-effect
  [action-name]
  (fn [state effect]
    (if (not (fight-ended? state))
      (-> state
          (process-hp action-name effect)
          (process-max-hp action-name effect)
          (process-mp action-name effect))
      state)))

(defn process-event
  [data [action-name effects]]
  (reduce (process-effect action-name) data effects))

(defn position-on-board?
  [x y]
  (and (nat-int? x)
       (< x mu/sizex)
       (nat-int? y)
       (< y mu/sizey)))

(defn get-active-board
  [state]
  (get-in state [:board :dungeons
                 (dec (get-in state [:board :player-position 0]))]))

(defn get-active-tile
  [data]
  (let [board (board/get-active-board (:board data))
        [_ x y] (get-in data [:board :player-position])]
    (:type (mu/get-tile board x y))))

(defn player-tile
  [state]
  (let [[_ x y] (get-in state [:board :player-position])]
    (mu/get-tile (board/get-active-board (:board state)) x y)))

(defn roll
  [n]
  (inc (rand-int n)))

(defn roll-dice
  ([n]
   (roll n))
  ([n s]
   (reduce + (take n (map #(%) (repeat #(roll-dice s)))))))

(defn advantage
  [n]
  (max (roll-dice n)
       (roll-dice n)))

(defn disadvantage
  [n]
  (min (roll-dice n)
       (roll-dice n)))

(defn get-enemy-stat
  [k]
  (let [enemy (get db/enemies k)]
    {:name k
     :ac (:ac enemy)
     :hp [(:hp enemy) (:hp enemy)]
     :mp [(:mp enemy) (:mp enemy)]}))

(defn start-fight
  [data enemy]
  (-> data
      (assoc :fight {:enemy (get-enemy-stat enemy)
                     :actions []})
      (add-message (str "You got attacked by a " enemy))))

(defn useable-item?
  [item]
  (let [v (get db/item-data item)]
    (or (contains? v :hp)
        (contains? v :mp)
        (contains? v :maxhp)
        (contains? (get v :properties #{}) :recipie)
        (contains? v :spell))))
