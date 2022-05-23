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
   [lum.game.utilities :as u]
   [lum.maputil :as mu]))

(defn roll-dice
  [n]
  (inc (rand-int n)))

(defn advantage
  [n]
  (max (roll-dice n)
       (roll-dice n)))

(defn disadvantage
  [n]
  (min (roll-dice n)
       (roll-dice n)))


(defn set-position
  [data [_ x y]]
  (let [data (assoc-in data [:player :position] [x y])]
    (loop [data data]
      (if (s/valid? :game/game data)
        data
        (recur (assoc-in data [:boards (dec (:level data))] (cavegen/get-dungeon)))))))

(defn position-on-board?
  [x y]
  (and (nat-int? x)
       (< x mu/sizex)
       (nat-int? y)
       (< y mu/sizey)))

(defn move-unchecked
  [data direction ]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc))
        [x y] (get-in new-data [:player :position])]
    (if (position-on-board? x y)
      new-data
      data)))

(defn change-active-tile
  [data new-type]
  (let [[x y] (get-in data [:player :position])]
    (assoc-in data [:boards
                    (dec (:level data))
                    (mu/position-to-n x y)
                    :type]
              new-type)))

(defn get-active-board
  [state]
  (get-in state [:boards (dec (:level state))]))

(defn get-active-tile
  [data]
  (let [board (get-active-board data)
        [x y] (get-in data [:player :position])]
    (:type (mu/get-tile board x y))))

(defn active-item-can-dig?
  [data]
  (let [weapon (get-in data [:player :equipment :right-hand])
        function (get-in db/item-effects [weapon :properties])]
    (some #{:digging} function)))

(defn pick-wall
  [data]
  (log/info (get-active-tile data))
  (if (and (= :wall (get-active-tile data))
           (active-item-can-dig? data)
           (< 0 (roll-dice 20)))
    (change-active-tile data :ground)
    data))

(defn move
  [data [_ direction]]
  (let [new-data (-> (move-unchecked data direction)
                     (pick-wall))]
;;    (s/explain :game/game new-data)
    (if (s/valid? :game/game new-data)
      new-data
      data)))

(defn new-board
  [data _]
  (assoc data :board (cavegen/get-dungeon)))

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
                    :items { "sword" 1}}}]
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
  (if (< (advantage 20) 5)
    ;;Start a fight every 20 turns
    (let [enemy (choose-enemy)]
      (-> data
          (assoc :fight {:enemy (get-enemy-stat enemy)
                         :actions []})
          (update :messages #(conj % (str "You got attacked by a " enemy)))))
    data))

(defn roll
  [n s]
  (reduce + (take n (map #(%) (repeat #(roll-dice s))))))

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




(defn find-tile
  [board tile]
  (mu/n-to-position (.indexOf board {:type tile})))

(defn set-to-tile
  [state tile]
  (assoc-in state [:player :position]
            (find-tile (get-active-board state) tile)))

(defn cavegen-when-required
  [state]
  (if (> (:level state) (count (:boards state)))
      (update state :boards #(conj % (cavegen/get-dungeon)))
      state))

(defn enter-next-level
  [state]
  (-> state
      (update :level inc)
      (cavegen-when-required)
      (set-to-tile :stair-up)))

(defn enter-previous-level
  [state]
  (if (not (= 1 (:level state)))
    (-> state
        (update :level dec)
        (set-to-tile :stair-down))
    state))


(defn player-tile
  [state]
  (let [[x y] (get-in state [:player :position])]
    (mu/get-tile (get-active-board state) x y)))

(defn look-for-item
  [state]
  (if (<= 16 (disadvantage 20))
    (item/change-items state {"herb" 1
                         "wooden stick" 1})
    state))

(defn activate
  [state _]
  (case (:type (player-tile state))
    :stair-down (enter-next-level state)
    :stair-up (enter-previous-level state)
    :ground (look-for-item state)
    state))


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
         {:activate [activate check-fight]
          :load-map [load-save/load-map]
          :move [move check-fight]
          :set-position [set-position]
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
