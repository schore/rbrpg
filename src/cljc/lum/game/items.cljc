(ns lum.game.items)

(defn enough?
  [items required-items]
  (every? (fn [[k v]] (<= v (get items k 0))) required-items))
