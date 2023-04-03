(ns lum.game.fight
  (:require
   [lum.game.game-database :as db]
   [lum.game.utilities :as u]
   [lum.game.player :as player]))

(defn enemy-allowed?
  [level [_ enemy-data]]
  (let [[from to] (get enemy-data :level [0 0])]
    (and (or (= 0 from)
             (>= level from))
         (or (= 0 to)
             (<= level to)))))

(defn possible-enemies
  [data]
  (let [level (u/get-level data)]
    (->> db/enemies
         (filter #(enemy-allowed? level %))
         (map first))))

(defn choose-enemy
  [data]
  (let [enemies (possible-enemies data)]
    (rand-nth enemies)))

(defn get-enemy-stat
  [k]
  (let [enemy (get db/enemies k)]
    {:name k
     :ac (:ac enemy)
     :hp [(:hp enemy) (:hp enemy)]
     :mp [(:mp enemy) (:mp enemy)]}))

(defn fight-ended?
  [data]
  (= 0 (get-in data [:fight :enemy :hp 0])))

(defn get-enemy
  [data]
  (get db/enemies (get-in data [:fight :enemy :name])))

(defn xp-to-level
  [xp]
  (let [required-xp [0 100 300 900 2700]]
    (count (filter #(>= xp %) required-xp))))

(defn level-up?
  [xp initial-xp]
  (> (xp-to-level xp) (xp-to-level initial-xp)))

(defn increase-stats-on-level-up
  [data initial-xp]
  (if (level-up? (get-in data [:player :xp]) initial-xp)
    (-> data
        (update-in [:player :hp 1] (partial + 6))
        (update-in [:player :mp 1] (partial + 3)))
    data))

(defn update-xp
  [data]
  (-> data
      (update-in [:player :xp] #(+ % (:xp (get-enemy data))))
      (increase-stats-on-level-up (get-in data [:player :xp]))))

(defn loot-items
  [data]
  (reduce (fn [data [item dice]]
            (if (>= (u/roll-dice 20) dice)
              (-> data
                  (update :player #(player/add-items % {item 1}))
                  (u/add-message (str "You looted a "  item)))
              data))
          data
          (partition 2 (:items (get-enemy data)))))

(defn hit?
  [hit-roll ac]
  (case hit-roll
    1 false
    20 true
    (> hit-roll ac)))

(defn damage-calc
  [hit-roll [n dice]]
  (u/roll-dice (if (= hit-roll 20)
                 (inc n) n)
               dice))

(defn attack-calc
  [ac weapon-damage]
  (let [hit-roll (u/roll-dice 20)]
    (if (hit? hit-roll ac)
      (damage-calc hit-roll weapon-damage)
      0)))

(defn player-attacks
  [data]
  (u/process-event
   data
   ["Beat"
    [(let [enemy-ac (get-in data [:fight :enemy :ac])
           weapon (get-in data [:player :equipment :right-hand])
           weapon-damage (get-in db/item-data [weapon :damage] [1 2])]
       {:target :enemy
        :hp (* -1 (attack-calc enemy-ac weapon-damage))})]]))

(defn get-armor-class
  [data]
  (let [equipment (get-in data [:player :equipment])]
    (max
     (reduce (fn [a [k item]]
               (+ a (get-in db/item-data [item :ac] 0)))
             0 equipment)
     (get-in data [:player :ac] 0))))

(defn get-enemy-attack-roles
  [data]
  (let [enemy (get-in data [:fight :enemy :name])]
    (get-in db/enemies [enemy :damage])))

(defn enemy-attacks
  [data]
  (if (> (get-in data [:fight :enemy :hp 0]) 0)
    (u/process-event
     data
     (let [player-ac (get-armor-class data)
           weapon-damage (get-enemy-attack-roles data)]
       ["Bite" [{:target :player
                 :hp (* -1 (attack-calc player-ac weapon-damage))}]]))
    data))

;; high level
(defn check-fight
  [data _]
  (if (< (u/advantage 20) 5)
    ;;Start a fight every 20 turns
    (u/start-fight data (choose-enemy data))
    data))

(defn check-fight-end
  [data _]
  (if (fight-ended? data)
    (-> data
        update-xp
        loot-items
        (dissoc :fight))
    data))

(defn attack
  [data _]
  (-> data
      player-attacks
      enemy-attacks))

(defn flea
  [data _]
  (if (> (u/roll-dice 20)
         10)
    (dissoc data :fight)
    (enemy-attacks data)))
