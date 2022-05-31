(ns lum.game.move
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]
   [lum.maputil :as mu]
   [lum.game.cavegen :as cavegen]
   [lum.game.item :as item]
   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]))


(defn active-item-can-dig?
  [data]
  (let [weapon (get-in data [:player :equipment :right-hand])
        function (get-in db/item-effects [weapon :properties])]
    (some #{:digging} function)))

(defn pick-wall
  [data]
  (log/info (u/get-active-tile data))
  (if (and (= :wall (u/get-active-tile data))
           (active-item-can-dig? data)
           (< 0 (u/roll-dice 20)))
    (u/change-active-tile data :ground)
    data))


(defn move-unchecked
  [data direction ]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc))
        [x y] (get-in new-data [:player :position])]
    (if (u/position-on-board? x y)
      new-data
      data)))

(defn find-tile
  [board tile]
  (mu/n-to-position (.indexOf board {:type tile})))


(defn set-to-tile
  [state tile]
  (assoc-in state [:player :position]
            (find-tile (u/get-active-board state) tile)))

(defn cavegen-when-required
  [state]
  (if (> (:level state) (count (:boards state)))
      (update state :boards #(conj % (cavegen/get-dungeon)))
      state))

(defn enter-next-level
  [state]
  (-> state
      (update :level inc)
      (cavegen-when-required)
      (set-to-tile :stair-up)))

(defn enter-previous-level
  [state]
  (if (not (= 1 (:level state)))
    (-> state
        (update :level dec)
        (set-to-tile :stair-down))
    state))

(defn look-for-item
  [state]
  (if (<= 16 (u/disadvantage 20))
    (u/add-items state {"herb" 1
                        "wooden stick" 1})
    state))

;; High level
(defn move
  [data [_ direction]]
  (let [new-data (-> (move-unchecked data direction)
                     (pick-wall))]
;;    (s/explain :game/game new-data)
    (if (s/valid? :game/game new-data)
      new-data
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
