(ns lum.game.view
  (:require
   [clojure.math :as math]
   [lum.maputil :as mu]
   [lum.game.dataspec]
   [lum.game.dungeon :as dungeon]))

(defn update-data
  [data]
  (let [[level x y] (get-in data [:board :player-position])]
    (update-in data [:board :dungeons (dec level)]
               (fn [board]
                 (dungeon/update-view board x y)))))

(defn process-view
  [data]
  (if (seq data)
    (-> data
        update-data)
    data))
