(ns lum.game.utilities
  (:require [lum.maputil :as mu]))

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

(defn process-hp
  [state action-name effect]
  (if (contains? effect :hp)
    (-> state
        (update :messages #(conj % (str action-name ": " (:hp effect) "hp")))
        (add-hp (:target effect) (:hp effect)))
    state))

(defn process-effect
  [action-name]
  (fn [state effect]
    (if (not (fight-ended? state))
      (-> state
          (process-hp action-name effect))
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

(defn change-active-tile
  [data new-type]
  (let [[x y] (get-in data [:player :position])]
    (assoc-in data [:boards
                    (dec (:level data))
                    (mu/position-to-n x y)
                    :type]
              new-type)))

(defn get-active-board
  [state]
  (get-in state [:boards (dec (:level state))]))

(defn get-active-tile
  [data]
  (let [board (get-active-board data)
        [x y] (get-in data [:player :position])]
    (:type (mu/get-tile board x y))))

(defn player-tile
  [state]
  (let [[x y] (get-in state [:player :position])]
    (mu/get-tile (get-active-board state) x y)))

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

(defn remove-if-empty
  [data item]
  (if (pos-int? (get-in data [:player :items item]))
    data
    (-> data
        (update-in [:player :items] #(dissoc % item))
        (update-in [:player :equipment]
                   #(filter-map (fn [[_ v]] (not= v item)) %)))))

(defn add-item
  [data item n]
  ;; nil is passed in case the k is not in the list
  (-> (update-in data [:player :items item] #(+ (if % % 0) n))
      (remove-if-empty item)))

(defn add-items
  [data used-items]
  (reduce (fn [data [item n]] (add-item data item n))
          data
          used-items))
