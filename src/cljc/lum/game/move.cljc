(ns lum.game.move
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]
   [lum.maputil :as mu]
   [lum.game.cavegen :as cavegen]
   [lum.game.load-save :as load]
   [clojure.spec.alpha :as s]
   [lum.game.utilities :as util]))

(defn active-item-can-dig?
  [data]
  (let [weapon (get-in data [:player :equipment :right-hand])
        function (get-in db/item-data [weapon :properties])]
    (some #{:digging} function)))

(defn pick-wall
  [data]
  (if (and (= :wall (u/get-active-tile data))
           (active-item-can-dig? data)
           (< 0 (u/roll-dice 20)))
    (u/change-active-tile data :ground)
    data))

(defn move-unchecked
  [data direction]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc))
        [x y] (get-in new-data [:player :position])]
    (if (u/position-on-board? x y)
      new-data
      data)))

(defn find-index
  [c f]
  (first (keep-indexed (fn [index element]
                         (when (f element) index))
                       c)))

(defn find-tile
  [board tile]
  (mu/n-to-position (find-index board #(= tile (:type %)))))

(defn set-to-tile
  [state tile]
  (assoc-in state [:player :position]
            (find-tile (u/get-active-board state) tile)))

(defn apply-map-effect
  [board effect]
  (let [[[x y] type & effect] effect]
    (case type
      :message (let [[message] effect]
                 (assoc-in board [(mu/position-to-n x y) :message] message))
      :enemy (let [[enemy] effect]
               (assoc-in board [(mu/position-to-n x y) :enemy] enemy))
      :item (let [[item] effect]
              (assoc-in board [(mu/position-to-n x y) :items item] 1))
      board)))

(defn load-effect
  [board level]
  (reduce apply-map-effect
          board
          (partition 3
                     (get-in db/special-maps [level :effects]
                             []))))

(defn load-special-map
  [level]
  (load/load-map-from-string (get-in db/special-maps [level :map])))

(defn cavegen-required?
  [state]
  (> (inc (:level state)) (count (:boards state))))

(defn enter-next-level
  [state]
  (if (cavegen-required? state)
    (update state :coeffects #(conj % [:enter-unknown-level (inc (:level state))]))
    (-> state
        (update :level inc)
        (set-to-tile :stair-up))))

(defn enter-previous-level
  [state]
  (if (not (= 1 (:level state)))
    (-> state
        (update :level dec)
        (set-to-tile :stair-down))
    state))

(defn add-found-item-messages
  [state item]
  (reduce (fn [s [item-name n]]
            (u/add-message s (str "You found " n " " item-name)))
          state item))

(defn get-items-on-tile
  [state]
  (get (u/player-tile state) :items {}))

(defn look-for-item
  [state]
  (let [items (get-items-on-tile state)]
    (-> state
        (u/add-items (into {} items))
        (u/update-active-tile #(dissoc % :items))
        (add-found-item-messages items))))

(defn scripted-message
  [data]
  (if-some [message (:message (u/player-tile data))]
    (u/add-message data message)
    data))

(defn enemy-encounter
  [data]
  (if-some [enemy (:enemy (u/player-tile data))]
    (u/start-fight data enemy)
    data))

(defn execute-scripts
  [data]
  (-> data
      scripted-message
      enemy-encounter))

;; High level
(defn move
  [data [_ direction]]
  (let [new-data (-> (move-unchecked data direction)
                     (pick-wall))]
;;    (s/explain :game/game new-data)
    (if (contains? #{:ground :stair-down :stair-up}
                   (u/get-active-tile new-data))
      (execute-scripts new-data)
      data)))

(defn activate
  [state _]
  (case (:type (u/player-tile state))
    :stair-down (enter-next-level state)
    :stair-up (enter-previous-level state)
    :ground (look-for-item state)
    state))

(defn set-position
  [data [_ x y]]
  (let [data (assoc-in data [:player :position] [x y])]
    (loop [data data]
      (if (s/valid? :game/game data)
        data
        (recur (assoc-in data [:boards (dec (:level data))] (cavegen/get-dungeon)))))))

(defn enter-unknown-level
  [data [_ level board]]
  (-> data
      (assoc-in [:boards (dec level)] board)
      (assoc :level level)
      (set-to-tile :stair-up)))
