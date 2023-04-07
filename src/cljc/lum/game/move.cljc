(ns lum.game.move
  (:require
   [lum.game.game-database :as db]
   [lum.game.board :as board]
   [lum.game.utilities :as u]
   [lum.game.player :as player]
   [lum.maputil :as mu]))

(defn pick-wall
  [data]
  (if (and (= :wall (board/get-active-tile (:board data)))
           (player/active-item-can-dig? (:player data))
           (< 0 (u/roll-dice 20)))
    (update data :board #(board/change-active-tile % :ground))
    data))

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

(defn add-found-item-messages
  [state item]
  (reduce (fn [s [item-name n]]
            (u/add-message s (str "You found " n " " item-name)))
          state item))

(defn get-items-on-tile
  [state]
  (get (board/player-tile (:board state)) :items {}))

(defn look-for-item
  [state]
  (let [items (get-items-on-tile state)]
    (-> state
        (update :player #(player/add-items % (into {} items)))
        (update :board (fn [board]
                         (board/update-active-tile board #(dissoc % :items))))
        (add-found-item-messages items))))

(defn scripted-message
  [data]
  (if-some [message (:message (board/player-tile (:board data)))]
    (u/add-message data message)
    data))

(defn enemy-encounter
  [data]
  (if-some [enemy (:enemy (board/player-tile (:board data)))]
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
  (let [new-data (-> data
                     (update :board #(board/move % direction))
                     (pick-wall))]
;;    (s/explain :game/game new-data)
    (if (contains? #{:ground :stair-down :stair-up}
                   (board/get-active-tile (:board new-data)))
      (execute-scripts new-data)
      data)))

(defn activate
  [state _]
  (case (board/get-active-tile (:board state))
    :stair-down (let [{board :board
                       effects :effects} (board/enter-next-level (:board state))]
                  (-> state
                      (assoc :board board)
                      (update :coeffects #(reduce conj % effects))))
    :stair-up (update state :board board/enter-previous-level)
    :ground (look-for-item state)
    state))

(defn enter-unknown-level
  [data [_ level board]]
  (update data :board #(board/enter-unknown-level % level board)))
