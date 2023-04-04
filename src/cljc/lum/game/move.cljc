(ns lum.game.move
  (:require
   [lum.game.game-database :as db]
   [lum.game.board :as board]
   [lum.game.utilities :as u]
   [lum.game.player :as player]
   [lum.maputil :as mu]))

(defn pick-wall
  [data]
  (if (and (= :wall (u/get-active-tile data))
           (player/active-item-can-dig? (:player data))
           (< 0 (u/roll-dice 20)))
    (u/change-active-tile data :ground)
    data))

(defn move-unchecked
  [data direction]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:board :player-position 1] dec)
                   :right (update-in data [:board :player-position 1] inc)
                   :up (update-in data [:board :player-position 2] dec)
                   :down (update-in data [:board :player-position 2] inc))
        [_ x y] (get-in new-data [:board :player-position])]
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
  (update-in state [:board :player-position]
             (fn [[level _ _]]
               (let [[x y] (find-tile (u/get-active-board state) tile)]
                 [level x y]))))

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

(defn cavegen-required?
  [state]
  (> (inc (board/get-level (:board state))) (count (get-in state [:board :dungeons]))))

(defn enter-next-level
  [state]
  (if (cavegen-required? state)
    (update state :coeffects #(conj % [:enter-unknown-level (inc (board/get-level (:board state)))]))
    (-> state
        (u/update-level inc)
        (set-to-tile :stair-up))))

(defn enter-previous-level
  [state]
  (if (not= 1 (board/get-level (:board state)))
    (-> state
        (u/update-level dec)
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
        (update :player #(player/add-items % (into {} items)))
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

(defn enter-unknown-level
  [data [_ level board]]
  (-> data
      (assoc-in [:board :dungeons (dec level)] board)
      (assoc-in [:board :player-position 0] level)
      (set-to-tile :stair-up)))
