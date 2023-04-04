(ns lum.game.board
  (:require
   [lum.maputil :as mu]))

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
