(ns lum.game.gamelogic
  (:require
   [clojure.core.async :as a :refer [<! >! chan close! go-loop]]
   [clojure.spec.alpha :as s]
   [clojure.string]
   [clojure.tools.logging :as log]
   [lum.game.cavegen :as cavegen]
   [lum.game.load-save :as load-save]
   [lum.game.dataspec]
   [lum.game.game-database :refer [enemies recipies item-effects]]
   [lum.game.update-data]
   [lum.game.game-database :as db]
   [lum.game.item :as item]
   [lum.game.move :as move]
   [lum.game.utilities :as u]
   [lum.maputil :as mu]))


(defn new-board
  [data _]
  (-> data
      (assoc-in [:boards (dec (:level data))] (cavegen/get-dungeon))
      (move/set-to-tile :ground)))

(defn initialize
  [_ _]
  (loop []
    (let [data {:boards [(cavegen/get-dungeon)]
                :level 1
                :messages '("Hello adventurer")
                :player {:position [10 10]
                         :ac 5
                         :xp 0
                         :hp [10 10]
                         :mp [3 3]
                         :equipment {}
                         :items {"sword" 1}}}]
      (if (s/valid? :game/game data)
        data
        (recur)))))

(defn fight-ended?
  [data]
  (= 0 (get-in data [:fight :enemy :hp 0])))

(defn game-over?
  [data]
  (= 0 (get-in data [:player :hp 0])))

(defn get-enemy-stat
  [k]
  (let [enemy (get enemies k)]
    {:name k
     :ac (:ac enemy)
     :hp [(:hp enemy) (:hp enemy)]
     :mp [(:mp enemy) (:mp enemy)]}))

(defn choose-enemy
  []
  (let [enemies (map first enemies)]
    (rand-nth enemies)))

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

(defn roll
  [n s]
  (reduce + (take n (map #(%) (repeat #(u/roll-dice s))))))

(defn damage-role
  [_ attack_role]
  (case attack_role
    1 0
    20 (roll 2 3)
    (roll 1 3)))

(defn attack-calc
  [ac n dice]
  (let [hit-roll (roll 1 20)
        hit? (case hit-roll
               1 false
               20 true
               (> hit-roll ac))
        n (if (= 20 hit-roll)
            (inc n)
            n)]
    (if hit?
      (roll n dice)
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

(defn attack
  [data _]
  (let  [actions [(player-attacks data)
                  (enemy-attacks data)]]
    (reduce u/process-event data actions)))

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

(defn check-fight-end
  [data _]
  (if (fight-ended? data)
    (-> data
        update-xp
        update-items
        (dissoc :fight))
    data))

(def basic-mode
  {:initialize [initialize]
   :load [load-save/load-game]
   :save [load-save/save-game]
   :equip [item/equip-item]
   :unequip [item/unequip-item]
   :nop []})

(def game-over-mode
  (merge basic-mode
         {}))

(def fight-mode
  (merge basic-mode
         {:attack [attack check-fight-end]
          :use-item [item/use-item]}))

(def move-mode
  (merge basic-mode
         {:activate [move/activate check-fight]
          :load-map [load-save/load-map]
          :move [move/move check-fight]
          :set-position [move/set-position]
          :new-board [new-board]
          :combine [item/combine]
          :use-item [item/use-item]}))

(defn get-mode-map
  [state]
  (cond
    (game-over? state) game-over-mode
    (contains? state :fight) fight-mode
    :else move-mode))

(defn process-actions
  [data action]
  (when (nil? (get (get-mode-map data) (keyword (first action))))
    (log/error "No entry defined " action))
  (reduce (fn [data f]
            (f data action))
          data (get (get-mode-map data)
                    (keyword (first action)) [])))

(defn game-master
  [input-chan]
  (let [out (chan)]
    (go-loop [data {}]
      (let [action (<! input-chan)
            new-data (-> data
                         (process-actions action)
                         (update :messages #(take 10 %)))]
        (if (some? action)
          (do
            (>! out new-data)
            (recur new-data))
          (do
            (close! out)
            data))))
    out))
