(ns lum.game.move
  (:require
   [lum.game.utilities :as u]
   [lum.game.game-database :as db]
   [clojure.tools.logging :as log]
   [clojure.spec.alpha :as s]))


(defn active-item-can-dig?
  [data]
  (let [weapon (get-in data [:player :equipment :right-hand])
        function (get-in db/item-effects [weapon :properties])]
    (some #{:digging} function)))

(defn pick-wall
  [data]
  (log/info (u/get-active-tile data))
  (if (and (= :wall (u/get-active-tile data))
           (active-item-can-dig? data)
           (< 0 (u/roll-dice 20)))
    (u/change-active-tile data :ground)
    data))


(defn move-unchecked
  [data direction ]
  (let [new-data (case (keyword direction)
                   :left (update-in data [:player :position 0] dec)
                   :right (update-in data [:player :position 0] inc)
                   :up (update-in data [:player :position 1] dec)
                   :down (update-in data [:player :position 1] inc))
        [x y] (get-in new-data [:player :position])]
    (if (u/position-on-board? x y)
      new-data
      data)))

;; High level
(defn move
  [data [_ direction]]
  (let [new-data (-> (move-unchecked data direction)
                     (pick-wall))]
;;    (s/explain :game/game new-data)
    (if (s/valid? :game/game new-data)
      new-data
      data)))
