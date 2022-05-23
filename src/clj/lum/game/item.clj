(ns lum.game.item
  (:require [lum.game.game-database :as db]
            [lum.game.utilities :as u]))


(defn enough-items?
  [data required-items]
  (let [items (get-in data [:player :items])]
    (every? (fn [[k v]] (<= v (get-in data [:player :items k] 0))) required-items)))

(defn remove-empty-items
  [data]
  (update-in data [:player :items]
             (fn [items] (u/filter-map (fn [[_ v]] (pos-int? v)) items))))


(defn change-item
  [data item n]
  ;; nil is passed in case the k is not in the list
  (update-in data [:player :items item] #(+ (if % % 0) n)))

(defn change-items
  [data used-items]
  (remove-empty-items (reduce (fn [a [item n]] (change-item a item n))
                              data
                              used-items)))


(defn unequip-items-not-in-inventory
  [state]
  (let [items (get-in state [:player :items])]
    (update-in state [:player :equipment]
               #(into {}
                      (filter (fn [[_ v]] (< 1 (get items v 0))) %)))))


;; High level functions
(defn equip-item
  [state [_ slot item]]
  (if (enough-items? state {item 1})
    (assoc-in state [:player :equipment (keyword slot)] item)
    state))

(defn unequip-item
  [state [_ slot]]
  (update-in state [:player :equipment] #(dissoc % (keyword slot))))


(defn use-item
  [data [_ item]]
  (if (enough-items? data {item 1})
    (-> data
        (change-items {item -1})
        (u/process-event [(str "Use item: " item) [(get db/item-effects item {})]])
        (unequip-items-not-in-inventory))
    data))

(defn combine
  [data [_ used-items]]
  (let [used-items (u/filter-map (fn [[_ v]] (pos-int? v)) used-items)
        new-item (get db/recipies used-items)]
    (if (enough-items? data used-items)
      (-> data
          (change-items (u/map-map (fn [[k v]] [k (* -1 v)]) used-items))
          (change-items (if new-item
                          {new-item 1}
                          {}))
          (unequip-items-not-in-inventory))
      data)))
