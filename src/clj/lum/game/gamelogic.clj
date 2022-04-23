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
   [lum.game.update-data]))

(defn set-position
  [data [_ x y]]
  (let [data (assoc-in data [:player :position] [x y])]
    (loop [data data]
      (if (s/valid? :game/game data)
        data
        (recur (assoc data :board (cavegen/get-dungeon)))))))

(defn move-unchecked
  [data direction ]
  (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc)))

(defn move
  [data [_ direction]]
  (let [new-data (move-unchecked data direction)]
    (if (s/valid? :game/game new-data)
      new-data
      data)))

(defn new-board
  [data _]
  (assoc data :board (cavegen/get-dungeon)))

(defn initialize
  [_ _]
  (loop []
    (let [data {:board (cavegen/get-dungeon)
           :messages '("Hello adventurer")
           :player {:position [10 10]
                    :ac 5
                    :xp 0
                    :hp [10 10]
                    :mp [3 3]
                    :equipment {}
                    :items {}}}]
      (if (s/valid? :game/game data)
        data
        (recur)))))

(defn fight-ended?
  [data]
  (= 0 (get-in data [:fight :enemy :hp 0])))

(defn game-over?
  [data]
  (= 0 (get-in data [:player :hp 0])))

(defn filter-map
  [f m]
  (into {} (filter f m)))

(defn remove-empty-items
  [data]
  (update-in data [:player :items]
             (fn [items] (filter-map (fn [[_ v]] (pos-int? v)) items))))

(defn change-item
  [data item n]
  ;; nil is passed in case the k is not in the list
  (update-in data [:player :items item] #(+ (if % % 0) n)))

(defn change-items
  [data used-items]
  (log/info (get-in data [:player :items]) used-items)
  (remove-empty-items (reduce (fn [a [item n]] (change-item a item n))
                              data
                              used-items)))

(defn get-target-keys
  [target & ext]
  (concat (case target
            :player [:player]
            :enemy [:fight :enemy])
          ext))

(defn add-with-boundaries
  [min max & vals]
  (let [s (reduce + vals)]
    (cond
      (< s min) min
      (> s max) max
      :else s)))

(defn process-hp
  [state action-name effect]
  (-> state
      (update :messages #(conj % (str action-name ": " (:hp effect) "hp")))
      (update-in (get-target-keys (:target effect) :hp)
                 (fn [[v max]] [(add-with-boundaries 0 max v (:hp effect)) max]))))

(defn process-effect
  [action-name]
  (fn [state effect]
      (if (not (fight-ended? state))
        (-> state
            (process-hp action-name effect))
        state)))

(defn process-event
  [data [action-name effects]]
  (log/info data action-name effects)
  (reduce (process-effect action-name) data effects))

(defn enough-items?
  [data required-items]
  (let [items (get-in data [:player :items])]
    (every? (fn [[k v]] (<= v (get-in data [:player :items k] 0))) required-items)))

(defn map-map
  [f m]
  (into {} (map f m)))


(defn combine
  [data [_ used-items]]
  (log/info (get-in [:player :items] used-items))
  (let [used-items (filter-map (fn [[_ v]] (pos-int? v)) used-items)
        new-item (get recipies used-items)]
    (if (enough-items? data used-items)
      (-> data
          (change-items (map-map (fn [[k v]] [k (* -1 v)]) used-items))
          (change-items (if new-item
                          {new-item 1}
                          {})))
      data)))

(defn use-item
  [data [_ item]]
  (log/info "use item " item)
  (if (enough-items? data {item 1})
    (-> data
        (change-items {item -1})
        (process-event [(str "Use item: " item) (get item-effects item {})]))
    data))

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
  (if (> (rand) 0.97)
    ;;Start a fight every 20 turns
    (let [enemy (choose-enemy)]
      (-> data
          (assoc :fight {:enemy (get-enemy-stat enemy)
                         :actions []})
          (update :messages #(conj % (str "You got attacked by a " enemy)))))
    data))



(defn roll
  [n s]
  (reduce + (take n (map #(%) (repeat #(inc (rand-int s)))))))

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
   [(let [enemy-ac (get-in data [:fight :enemy :ac])]
      {:target :enemy
       :hp (* -1 (attack-calc enemy-ac 1 3))})]])

(defn enemy-attacks
  [data]
  (let [player-ac (get-in data [:player :ac])]
    ["Bite" [{:target :player
              :hp (* -1 (attack-calc player-ac 1 2))}]]))

(defn attack
  [data _]
  (let  [actions [(player-attacks data)
                  (enemy-attacks data)]]
    (reduce process-event data actions)))

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
               (log/info items)
               (let [loot (:items (get-enemy data))]
                 (log/info loot)
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

(defn equip-item
  [state [_ slot item]]
  (if (enough-items? state {item 1})
    (assoc-in state [:player :equipment slot] item)
    state))


(def basic-mode
  {:initialize [initialize]
   :load [load-save/load-game]
   :save [load-save/save-game]
   :equip [equip-item]
   :nop []})

(def game-over-mode
  (merge basic-mode
         {}))

(def fight-mode
  (merge basic-mode
         {:attack [attack check-fight-end]
          :use-item [use-item]}))

(def move-mode
  (merge basic-mode
         {:load-map [load-save/load-map]
          :move [move check-fight]
          :set-position [set-position]
          :new-board [new-board]
          :combine [combine]
          :use-item [use-item]}))

(defn get-mode-map
  [state]
  (cond
    (game-over? state) game-over-mode
    (contains? state :fight) fight-mode
    :else move-mode))


(defn process-actions
  [data action]
  (if action
    (do
      (when (nil? (get (get-mode-map data) (keyword (first action))))
        (log/error "No entry defined " action))
      (reduce (fn [data f]
                (f data action))
              data (get (get-mode-map data)
                        (keyword (first action)) [])))
    data))


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
