(ns lum.game.magic
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]
   [lum.game.board :as board]
   [lum.game.fight :as fight]))

(defn known-spell?
  [state spell]
  (contains? (get-in state [:player :spells])
             spell))

(defn enough-mp?
  [state spell]
  (let [required-mp (get-in db/spells [spell :mp])
        current-mp (get-in state [:player :mp 0])]
    (>= current-mp required-mp)))

(defn calc-mp
  [spell]
  {:target :player
   :mp (* -1 (:mp spell))})

(defn calc-damage
  [spell]
  (when-let [damage (:damage spell)]
    {:target (:target spell)
     :hp (* -1 (apply u/roll-dice damage))}))

(defn calc-hp
  [spell]
  (when-let [hp (:hp spell)]
    {:target (:target spell)
     :hp hp}))

(defn calc-target
  [spell]
  (filter some? [(calc-mp spell)
                 (calc-damage spell)
                 (calc-hp spell)]))

(defn spell-possible-to-cast?
  [state spell]
  (or (= (:target (get db/spells spell)) :player)
      (contains? state :fight)))

(defn map-effect
  [state spell]
  (if (:mapping? spell)
    (update state :board
            (fn [board]
              (board/update-active-board board
                                         (fn [b]
                                           (into [] (map #(assoc % :visible? true) b))))))
    state))

;;
(defn cast-spell
  [state [_ spell]]
  (if (and (known-spell? state spell)
           (enough-mp? state spell)
           (spell-possible-to-cast? state spell))
    (let [spell-data (get db/spells spell)]
      (-> state
          (map-effect spell-data)
          (u/process-event [spell (calc-target spell-data)])))
    state))

(defn cast-spell-during-fight
  [state [_ spell]]
  (-> state
      (cast-spell [_ spell])
      (fight/enemy-attacks)))
