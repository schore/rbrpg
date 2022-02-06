(ns lum.game.gamelogic
  (:require
   [lum.game.cavegen :as cavegen]
   [lum.maputil :as mu]
   [lum.game.dataspec]
   [lum.game.update-data]
   [clojure.string]
   [clojure.spec.alpha :as s]
   [clojure.core.async :as a :refer [chan go go-loop <! >! close!]]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]))

(defn set-position
  [data [_ x y]]
  (assoc-in data [:player :position] [x y]))

(defn move
  [data [_ direction]]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc))
        position (get-in new-data [:player :position])]
    (if (and (s/valid? :game/position position)
             (= :ground
                (:type (mu/get-tile (:board new-data)
                                    (get-in new-data [:player :position 0])
                                    (get-in new-data [:player :position 1])))))
      new-data
      data)))

(defn pad [n pad coll]
  (take n (concat coll (repeat pad))))

(defn load-map-from-string
  [inp]
  (->> (clojure.string/split-lines inp)
       (map (fn [line]
              (->> (seq line)
                   (map (fn [c]
                          (case c
                            \  {:type :ground}
                            \. {:type :ground}
                            \# {:type :wall}
                            {:type :wall}))))))
       (map (fn [line]
              (pad mu/sizex {:type :wall} line)))
       flatten
       (pad (* mu/sizex mu/sizey) {:type :wall})))

(defn new-board
  [data _]
  (assoc data :board (cavegen/get-dungeon)))

(defn initialize
  [_ _]
  {:board (cavegen/get-dungeon)
   :messages '("Hello adventurer")
   :player {:position [10 10]
            :ac 5
            :xp 0
            :hp [10 10]
            :mp [3 3]}})

(defn load-map
  [data [_ file]]
  (if-let [mf (try
                (slurp (io/resource file))
                (catch Exception e (log/error "Exception thrown " (.getMessage e))))]
    (assoc data :board (load-map-from-string mf))
    data))

(defn check-fight
  [data _]
  (if (> (rand) 0.97)
    ;;Start a fight every 20 turns
    (-> data
        (assoc :fight {:enemy {:name "Bat"
                               :ac 10
                               :hp [2 2]
                               :mp [0 0]}
                       :actions []})
        (update :messages #(conj % "You got attacked by a Bat")))
    data))

(defn fight-ended?
  [data]
  (= 0 (get-in data [:fight :enemy :hp 0])))

(defn game-over?
  [data]
  (= 0 (get-in data [:palyer :hp])))

(defn process-effect
  [action-name]
  (fn [state {:keys [target stat n]}]
    (let [update-field (conj (case target
                               :player [:player]
                               :enemy [:fight :enemy])
                             stat)]
      (if (not (fight-ended? state))
        (-> state
            (update :messages #(conj % (str action-name ": " (* -1 n) " " (str stat))))
            (update-in update-field (fn [[v max]]
                                      (let [nv (+ v n)
                                            cv (cond
                                                 (< nv 0) 0
                                                 (> nv max) max
                                                 :else nv)]
                                        [cv max]))))
        state))))

(defn process-event
  [data [action-name effects]]
  (reduce (process-effect action-name) data effects))

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
       :stat :hp
       :n (* -1 (attack-calc enemy-ac 1 3))})]])

(defn enemy-attacks
  [data]
  (let [player-ac (get-in data [:player :ac])]
    ["Bite" [{:target :player
              :stat :hp
              :n (* -1 (attack-calc player-ac 1 2))}]]))

(defn attack
  [data _]
  (let  [actions [(player-attacks data)
                  (enemy-attacks data)]]
    (reduce process-event data actions)))

(defn update-xp
  [data]
  (update-in data [:player :xp] inc))

(defn check-fight-end
  [data _]
  (if (fight-ended? data)
    (-> data
        update-xp
        (dissoc :fight))
    data))

(defn load
  [state [_ new-data]]
  (if (s/valid? :game/game new-data)
    new-data
    state))

(def basic-mode
  {:initialize [initialize]
   :load [load]
   :nop []})


(def game-over-mode
  (merge basic-mode
         {}))



(def fight-mode
  (merge basic-mode
         {:attack [attack check-fight-end]}))

(def move-mode
  (merge basic-mode
         {:load-map [load-map]
          :move [move check-fight]
          :set-position [set-position]
          :new-board [new-board]}))

(defn get-mode-map
  [state]
  (cond
    (game-over? state) game-over-mode
    (contains? state :fight) fight-mode
    :else move-mode))

(defn enforce-spec
  [f]
  (fn [data command]
    (let [new-data (f data command)]
      (if (s/valid? :game/game new-data)
        new-data
        (do
          (log/error (s/explain-str :game/game new-data))
          data)))))

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

(defn board-update
  [data new-data]
  (if (not= (:board data)
            (:board new-data))
    [:new-board (:board new-data)]
    nil))

(defn player-move
  [data new-data]
  (when (not= (get-in data [:player :position])
              (get-in new-data [:player :position]))
    (let [[x y] (get-in new-data [:player :position])]
      [:player-move x y])))

(defn game-master
  [input-chan]
  (let [out (chan)]
    (go-loop [data {}]
      (let [action (<! input-chan)
            new-data (-> data
                         (process-actions action)
                         (update :messages #(take 10 %)))]
        ;    updates (lum.game.update-data/calc-updates data new-data)]
        (if (some? action)
          (do
            ;;(doseq [update updates] (>! out update))
            (>! out new-data)
            (recur new-data))
          (do
            (close! out)
            data))))
    out))
