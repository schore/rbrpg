(ns lum.game.board)

(defn get-level
  [board]
  (get-in board [:player-position 0]))
