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



(defn process-hp
  [state action-name effect]
  (if (contains? effect :hp)
    (-> state
        (update :messages #(conj % (str action-name ": " (:hp effect) "hp")))
        (update-in (get-target-keys (:target effect) :hp)
                   (fn [[v max]] [(add-with-boundaries 0 max v (:hp effect)) max])))
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

(defn roll-dice
  [n]
  (inc (rand-int n)))

(defn advantage
  [n]
  (max (roll-dice n)
       (roll-dice n)))

(defn disadvantage
  [n]
  (min (roll-dice n)
       (roll-dice n)))
