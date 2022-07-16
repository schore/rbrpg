(ns lum.game.magic
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]))

(defn attack-spell
  [state [_ spell]]
  (let [{:keys [target damage mp]} (get db/spells spell)]
    (println "spell " mp)
    (-> state
        (u/process-event [spell [{:target :player
                                  :mp (* -1 mp)}
                                 {:target target
                                  :hp (* -1 (apply u/roll-dice damage))}]]))))
