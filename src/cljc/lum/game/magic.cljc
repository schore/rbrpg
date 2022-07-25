(ns lum.game.magic
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]))

(defn known-spell?
  [state spell]
  (contains? (get-in state [:player :spells])
             spell))

(defn enough-mp?
  [state spell]
  (let [required-mp (get-in db/spells [spell :mp])
        current-mp (get-in state [:player :mp 0])]
    (>= current-mp required-mp)))

;;
(defn attack-spell
  [state [_ spell]]
  (if (and (known-spell? state spell)
           (enough-mp? state spell))
    (let [{:keys [target damage mp]} (get db/spells spell)]
      (-> state
          (u/process-event [spell [{:target :player
                                    :mp (* -1 mp)}
                                   {:target target
                                    :hp (* -1 (apply u/roll-dice damage))}]])))
    state))
