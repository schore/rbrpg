(ns lum.game.player
  (:require [lum.game.items :as items]
            [lum.game.utilities :as u]))

(defn- unequip-items-not-in-inventory
  [player item]
  (update player :equipment
          #(u/filter-map (fn [[_ v]] (not= v item)) %)))

(defn combine
  [player used-items]
  (reduce unequip-items-not-in-inventory
          (update player :items #(items/combine % used-items))
          (map first used-items)))

(defn equip-item
  [player slot item]
  (if (items/enough? (:items player) {item 1})
    (assoc-in player [:equipment (keyword slot)] item)
    player))

(defn unequip-item
  [player slot]
  (update player :equipment #(dissoc % (keyword slot))))
