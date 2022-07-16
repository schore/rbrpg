(ns lum.game.magic
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]))

(defn attack-spell
  [state [_ spell]]
  (let [{:keys [target damage]} (get db/spells spell)]
    (-> state
        (u/process-event [spell [{:target target
                                  :hp (* -1 (apply u/roll-dice damage))}]]))))
