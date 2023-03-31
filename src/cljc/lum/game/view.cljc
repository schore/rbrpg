(ns lum.game.view
  (:require
   [clojure.math :as math]
   [lum.maputil :as mu]
   [lum.game.dataspec]
   [lum.game.board :as board]))

(defn update-data
  [data]
  (let [[x y] (get-in data [:player :position])]
    (update-in data [:boards (dec (:level data))]
               (fn [board]
                 (board/update-board board x y)))))

(defn process-view
  [data]
  (if (seq data)
    (-> data
        update-data)
    data))
