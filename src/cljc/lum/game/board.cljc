(ns lum.game.board
  (:require
   [lum.maputil :as mu]
   [lum.game.utilities :as u]))

(defn get-level
  [board]
  (get-in board [:player-position 0]))

(defn update-level
  [board f]
  (update-in board [:player-position 0] f))

(defn get-position
  [board]
  (let [[_ x y] (:player-position board)]
    [x y]))

(defn update-active-tile
  [board f]
  (let [[x y] (get-position board)]
    (update-in board [:dungeons
                      (dec (get-level board))
                      (mu/position-to-n x y)]
               f)))

(defn update-active-board
  [board f]
  (update-in board [:dungeons (dec (get-level board))] f))

(defn change-active-tile
  [board new-type]
  (update-active-tile board #(assoc % :type new-type)))

(defn get-active-board
  [board]
  (get-in board [:dungeons (dec (get-level board))]))

(defn player-tile
  [board]
  (let [[x y] (get-position board)]
    (mu/get-tile (get-active-board board) x y)))

(defn get-active-tile
  [board]
  (:type (player-tile board)))

(defn move
  [board direction]
  (let [new-board (case (keyword direction)
                    :left (update-in board [:player-position 1] dec)
                    :right (update-in board [:player-position 1] inc)
                    :up (update-in board [:player-position 2] dec)
                    :down (update-in board [:player-position 2] inc))
        [x y] (get-position new-board)]
    (if (u/position-on-board? x y)
      new-board
      board)))
