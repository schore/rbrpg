(ns lum.game.board)

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
