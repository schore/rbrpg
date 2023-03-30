(ns lum.game.player
  (:require [lum.game.items :as items]
            [lum.game.utilities :as u]
            [lum.game.game-database :as db]))

(defn- unequip-items-not-in-inventory
  [player item]
  (update player :equipment
          #(u/filter-map (fn [[_ v]] (not= v item)) %)))

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

(defn add-recipie
  [player recipie]
  (if (some? recipie)
    (update player :recepies #(distinct (conj % recipie)))
    player))

(defn- get-recipie-to-learn
  [item]
  (if (contains? (:properties item) :recipie)
    (rand-nth (keys db/recipies))
    nil))

(defn- get-item-hint
  [player]
  (second (get db/recipies (first (get player :recepies)))))

(defn- use-item-message
  [player item]
  (let [i (get db/item-data item)]
    (cond-> []
      (contains? i :hp) (conj (str "HP: " (:hp i)))
      (contains? i :mp) (conj (str "MP: " (:mp i)))
      (contains? i :maxhp) (conj (str "Maxhp: " (:maxhp i)))
      (contains? i :spell) (conj (str "Learned spell: " (:spell i)))
      (contains? (:properties i) :recipie) (conj (get-item-hint player))
      true (conj (str "Use item: " item)))))

(defn use-item
  [player item]
  (if (and (items/enough? (:items player) {item 1})
           (u/useable-item? item))
    (let [i (get db/item-data item {})
          player (-> player
                     (add-items {item -1})
                     (add-hp (get i :hp 0))
                     (add-mp (get i :mp 0))
                     (add-max-hp (get i :maxhp 0))
                     (add-spell (get i :spell ""))
                     (add-recipie (get-recipie-to-learn i)))]
      {:player player :messages (use-item-message player item)})
    {:player player :messages []}))

(defn combine
  [player used-items]
  (let [used-items (items/clear-empty-items used-items)]
    (reduce unequip-items-not-in-inventory
            (-> player
                (add-recipie (when (and (items/enough? (:items player) used-items)
                                        (contains? db/recipies used-items))
                               used-items))
                (update :items #(items/combine % used-items)))
            (map first used-items))))
