(ns lum.game.item
  (:require [lum.game.game-database :as db]
            [lum.game.utilities :as u]
            [lum.game.items :as items]
            [lum.game.player :as player]))

(defn enough-items?
  [data required-items]
  (items/enough? (get-in data [:player :items]) required-items))

(defn add-recipie
  [data recipie]
  (update data :recepies #(distinct (conj % recipie))))

;; High level functions
(defn equip-item
  [state [_ slot item]]
  (update state :player #(player/equip-item % slot item)))

(defn unequip-item
  [state [_ slot]]
  (update state :player #(player/unequip-item % slot)))

(defn learn-recipie-from-item
  [data item]
  (if (contains? (get-in db/item-data [item :properties]) :recipie)
    (let [recipie (rand-nth (keys db/recipies))
          msg (second (get db/recipies recipie))]
      (-> data
          (u/add-message msg)
          (add-recipie recipie)))
    data))

(defn add-use-message
  [data item]
  (let [i (get db/item-data item)]
    (cond-> data
      (contains? i :hp) (u/add-message (str "HP: " (:hp i)))
      (contains? i :mp) (u/add-message (str "MP: " (:mp i)))
      (contains? i :maxhp) (u/add-message (str "Maxhp: " (:maxhp i)))
      (contains? i :spell) (u/add-message (str "Learned spell: " (:spell i)))
      true (u/add-message (str "Use item: " item)))))

(defn use-item
  [data [_ item]]
  (if (and (enough-items? data {item 1})
           (u/useable-item? item))
    (-> data
        (update :player #(player/use-item % item))
        (add-use-message item)
        (learn-recipie-from-item item))
    data))

(defn clear-empty-items
  [items]
  (u/filter-map (fn [[_ v]] (pos-int? v)) items))

(defn combine
  [data [_ used-items]]
  (update data :player #(player/combine % used-items)))

(defn remember-recipies
  [data [_ used-items]]
  (let [used-items (clear-empty-items used-items)]
    (if (and (enough-items? data used-items)
             (contains? db/recipies used-items))
      (add-recipie data used-items)
      data)))
