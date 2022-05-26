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
