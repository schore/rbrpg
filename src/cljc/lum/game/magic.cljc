(ns lum.game.magic
  (:require
   [lum.game.utilities :as u]))

(defn attack-spell
  [state [_ spell]]
  (-> state
      (u/process-event [spell [{:target :enemy
                                :hp (* -1 (u/roll-dice 3 6))}]])))
