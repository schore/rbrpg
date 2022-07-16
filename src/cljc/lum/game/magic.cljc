(ns lum.game.magic
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]))

(defn known-spell?
  [state spell]
  (contains? (get-in state [:player :spells])
             spell))
;;
(defn attack-spell
  [state [_ spell]]
  (if (known-spell? state spell)
    (let [{:keys [target damage mp]} (get db/spells spell)]
      (-> state
          (u/process-event [spell [{:target :player
                                    :mp (* -1 mp)}
                                   {:target target
                                    :hp (* -1 (apply u/roll-dice damage))}]])))
    state))
