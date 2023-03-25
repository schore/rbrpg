(ns lum.game.player
  (:require [lum.game.items :as items]
            [lum.game.utilities :as u]
            [lum.game.game-database :as db]))

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

(defn add-items
  [player items]
  (reduce unequip-items-not-in-inventory
          (update player :items
                  #(items/add-items % items))
          (map first items)))

(defn add-hp
  [player n]
  (update player :hp
          (fn [[v max]]
            [(u/add-with-boundaries 0 max v n) max])))

(defn add-mp
  [player n]
  (update player :mp
          (fn [[v max]]
            [(u/add-with-boundaries 0 max v n) max])))

(defn add-max-hp
  [player n]
  (update player :hp
          (fn [[v max]] [(+ v n) (+ max n)])))

(defn add-spell
  [player spell]
  (if (not= spell "")
    (update player :spells #(conj % spell))
    player))

(defn use-item
  [player item]
  (let [i (get db/item-data item {})]
    (-> player
        (add-items {item -1})
        (add-hp (get i :hp 0))
        (add-mp (get i :mp 0))
        (add-max-hp (get i :maxhp 0))
        (add-spell (get i :spell "")))))
