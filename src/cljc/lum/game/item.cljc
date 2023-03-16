(ns lum.game.item
  (:require [lum.game.game-database :as db]
            [lum.game.utilities :as u]
            [lum.game.items :as items]))

(defn enough-items?
  [data required-items]
  (items/enough? (get-in data [:player :items]) required-items))

(defn incriedients-2-item
  [incriedients]
  (get-in db/recipies [incriedients 0]))

(defn add-recipie
  [data recipie]
  (update data :recepies #(distinct (conj % recipie))))

;; High level functions
(defn equip-item
  [state [_ slot item]]
  (if (enough-items? state {item 1})
    (assoc-in state [:player :equipment (keyword slot)] item)
    state))

(defn unequip-item
  [state [_ slot]]
  (update-in state [:player :equipment] #(dissoc % (keyword slot))))

(defn learn-recipie-from-item
  [data item]
  (if (contains? (get-in db/item-data [item :properties]) :recipie)
    (let [recipie (rand-nth (keys db/recipies))
          msg (second (get db/recipies recipie))]
      (-> data
          (u/add-message msg)
          (add-recipie recipie)))
    data))

(defn add-spell
  [data item]
  (if-let [spell (get-in db/item-data [item :spell])]
    (-> data
        (u/add-message (str "Learned spell " spell))
        (update-in [:player :spells] #(conj % spell)))
    data))

(defn use-item
  [data [_ item]]
  (if (and (enough-items? data {item 1})
           (u/useable-item? item))
    (-> data
        (u/add-items {item -1})
        (u/process-event [(str "Use item: " item) [(get db/item-data item {})]])
        (learn-recipie-from-item item)
        (add-spell item))
    data))

(defn clear-empty-items
  [items]
  (u/filter-map (fn [[_ v]] (pos-int? v)) items))

(defn combine
  [data [_ used-items]]
  (let [used-items (clear-empty-items used-items)
        new-item (incriedients-2-item used-items)]
    (if (enough-items? data used-items)
      (-> data
          (u/add-items (u/map-map (fn [[k v]] [k (* -1 v)]) used-items))
          (u/add-items (if new-item
                         {new-item 1}
                         {})))
      data)))

(defn remember-recipies
  [data [_ used-items]]
  (let [used-items (clear-empty-items used-items)]
    (if (and (enough-items? data used-items)
             (contains? db/recipies used-items))
      (add-recipie data used-items)
      data)))
