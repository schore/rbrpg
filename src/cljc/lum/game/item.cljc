(ns lum.game.item
  (:require [lum.game.game-database :as db]
            [lum.game.utilities :as u]))

(defn enough-items?
  [data required-items]
  (every? (fn [[k v]] (<= v (get-in data [:player :items k] 0))) required-items))

;; High level functions
(defn equip-item
  [state [_ slot item]]
  (if (enough-items? state {item 1})
    (assoc-in state [:player :equipment (keyword slot)] item)
    state))

(defn unequip-item
  [state [_ slot]]
  (update-in state [:player :equipment] #(dissoc % (keyword slot))))

(defn add-hint
  [data item]
  (if (contains? (get-in db/item-data [item :properties]) :hint)
    (u/add-message data (rand-nth db/hints))
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
  (if (enough-items? data {item 1})
    (-> data
        (u/add-items {item -1})
        (u/process-event [(str "Use item: " item) [(get db/item-data item {})]])
        (add-hint item)
        (add-spell item))
    data))

(defn clear-empty-items
  [items]
  (u/filter-map (fn [[_ v]] (pos-int? v)) items))

(defn combine
  [data [_ used-items]]
  (let [used-items (clear-empty-items used-items)
        new-item (get db/recipies used-items)]
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
    (println used-items)
    (if (and (enough-items? data used-items)
             (contains? db/recipies used-items))
      (update data :recepies #(distinct (conj % used-items)))
      data)))
