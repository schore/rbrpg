(ns lum.game.move
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]
   [lum.maputil :as mu]
   [lum.game.cavegen :as cavegen]
   [lum.game.load-save :as load]
   [clojure.spec.alpha :as s]
   [lum.game.utilities :as util]))

(defn active-item-can-dig?
  [data]
  (let [weapon (get-in data [:player :equipment :right-hand])
        function (get-in db/item-data [weapon :properties])]
    (some #{:digging} function)))

(defn pick-wall
  [data]
  (if (and (= :wall (u/get-active-tile data))
           (active-item-can-dig? data)
           (< 0 (u/roll-dice 20)))
    (u/change-active-tile data :ground)
    data))

(defn move-unchecked
  [data direction]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc))
        [x y] (get-in new-data [:player :position])]
    (if (u/position-on-board? x y)
      new-data
      data)))

(defn find-index
  [c f]
  (first (keep-indexed (fn [index element]
                         (when (f element) index))
                       c)))

(defn find-tile
  [board tile]
  (mu/n-to-position (find-index board #(= tile (:type %)))))

(defn set-to-tile
  [state tile]
  (assoc-in state [:player :position]
            (find-tile (u/get-active-board state) tile)))

(defn add-map
  [state map]
  (update state :boards #(conj (into [] %) map)))

(defn apply-map-effect
  [data effect]
  (let [[[x y] type & effect] effect]
    (case type
      :message (let [[message & rest] effect]
                 [(util/change-tile data x y #(assoc % :message message))
                  rest])
      :enemy (let [[enemy & rest] effect]
               [(util/change-tile data x y #(assoc % :enemy enemy))
                rest])
      :item (let [[item & rest] effect]
              [(util/change-tile data x y #(assoc-in % [:items item] 1))
               rest])
      [data []])))

(defn load-effect
  [data]
  (let [level (:level data)
        effects (get-in db/special-maps [level :effects] [])]
    (loop [data data
           effects effects]
      (if (empty? effects)
        data
        (let [[data effects] (apply-map-effect data effects)]
          (recur data effects))))))

(defn load-special-map
  [data]
  (let [level (:level data)]
    (-> data
        (add-map (load/load-map-from-string (get-in db/special-maps [level :map])))
        (load-effect))))

(defn cavegen-when-required
  [state]
  (let [level (:level state)]
    (cond
      (<= level (count (:boards state))) state
      (contains? db/special-maps level) (load-special-map state)
      :else (add-map state (cavegen/get-dungeon)))))

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

(defn items-to-add
  [current-level thrown-dice]
  (->> db/items-on-ground
       (filter (fn [[_ {:keys [level dice]}]] (and (>= current-level (first level))
                                                   (<= current-level (second level))
                                                   (>= thrown-dice dice))))
       (map first)))

(defn add-found-item-messages
  [state item]
  (reduce (fn [s [item-name n]]
            (u/add-message s (str "You found " n " " item-name)))
          state item))

(defn get-items-on-tile
  [state]
  (get (u/player-tile state) :items {}))

(defn look-for-item
  [state]
  (let [items (get-items-on-tile state)]
    (-> state
        (u/add-items (into {} items))
        (u/update-active-tile #(dissoc % :items))
        (add-found-item-messages items))))

(defn scripted-message
  [data]
  (if-some [message (:message (u/player-tile data))]
    (u/add-message data message)
    data))

(defn enemy-encounter
  [data]
  (if-some [enemy (:enemy (u/player-tile data))]
    (u/start-fight data enemy)
    data))

(defn execute-scripts
  [data]
  (-> data
      scripted-message
      enemy-encounter))

;; High level
(defn move
  [data [_ direction]]
  (let [new-data (-> (move-unchecked data direction)
                     (pick-wall))]
;;    (s/explain :game/game new-data)
    (if (contains? #{:ground :stair-down :stair-up}
                   (u/get-active-tile new-data))
      (execute-scripts new-data)
      data)))

(defn activate
  [state _]
  (case (:type (u/player-tile state))
    :stair-down (enter-next-level state)
    :stair-up (enter-previous-level state)
    :ground (look-for-item state)
    state))

(defn set-position
  [data [_ x y]]
  (let [data (assoc-in data [:player :position] [x y])]
    (loop [data data]
      (if (s/valid? :game/game data)
        data
        (recur (assoc-in data [:boards (dec (:level data))] (cavegen/get-dungeon)))))))
