(ns lum.game.fight
  (:require [lum.game.utilities :as u]
            [lum.game.game-database :as db]))

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
