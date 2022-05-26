(ns lum.game.fight
  (:require
   [lum.game.game-database :as db :refer [enemies]]
   [lum.game.utilities :as u]))

(defn choose-enemy
  []
  (let [enemies (map first db/enemies)]
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
  (get enemies (get-in data [:fight :enemy :name])))

(defn update-xp
  [data]
  (update-in data [:player :xp]
             #(+ % (:xp (get-enemy data)))))



(defn update-items
  [data]
  (update-in data [:player :items]
             (fn [items]
               (let [loot (:items (get-enemy data))]
                 (reduce (fn [acc item]
                           (assoc acc item (inc (get acc item 0))))
                         items loot)))))


(defn attack-calc
  [ac n dice]
  (let [hit-roll (u/roll-dice 20)
        hit? (case hit-roll
               1 false
               20 true
               (> hit-roll ac))
        n (if (= 20 hit-roll)
            (inc n)
            n)]
    (if hit?
      (u/roll-dice n dice)
      0)))

(defn player-attacks
  [data]
  ["Beat"
   [(let [enemy-ac (get-in data [:fight :enemy :ac])
          weapon (get-in data [:player :equipment :right-hand])
          [n dice] (get-in db/item-effects [weapon :damage] [1 3])]
      {:target :enemy
       :hp (* -1 (attack-calc enemy-ac n dice))})]])

(defn get-armor-class
  [data]
  (let [equipment (get-in data [:player :equipment :body])]
    (get-in db/item-effects [equipment :ac]
            (get-in data [:player :ac]))))

(defn get-enemy-attack-roles
  [data]
  (let [enemy (get-in data [:fight :enemy :name])]
    (get-in db/enemies [enemy :damage])))



(defn enemy-attacks
  [data]
  (let [player-ac (get-armor-class data)
        [n dice] (get-enemy-attack-roles data)]
    ["Bite" [{:target :player
              :hp (* -1 (attack-calc player-ac n dice))}]]))


;; high level
(defn check-fight
  [data _]
  (if (< (u/advantage 20) 5)
    ;;Start a fight every 20 turns
    (let [enemy (choose-enemy)]
      (-> data
          (assoc :fight {:enemy (get-enemy-stat enemy)
                         :actions []})
          (update :messages #(conj % (str "You got attacked by a " enemy)))))
    data))

(defn check-fight-end
  [data _]
  (if (fight-ended? data)
    (-> data
        update-xp
        update-items
        (dissoc :fight))
    data))


(defn attack
  [data _]
  (let  [actions [(player-attacks data)
                  (enemy-attacks data)]]
    (reduce u/process-event data actions)))
