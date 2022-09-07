(ns lum.game.view
  (:require [lum.game.utilities :as u]
            [clojure.spec.alpha :as s]
            [lum.game.dataspec]))

(defn process-view
  [data]
  (if (s/valid? :game/game data)
    (-> data
        (u/update-active-tile #(assoc % :visible? true)))
    data))
