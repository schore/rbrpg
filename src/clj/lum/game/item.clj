(ns lum.game.item
  (:require [lum.game.game-database :as db]
            [lum.game.utilities :as u]))


(defn enough-items?
  [data required-items]
  (every? (fn [[k v]] (<= v (get-in data [:player :items k] 0))) required-items))


(defn remove-if-empty
  [data item]
  (if (pos-int? (get-in data [:player :items item]))
    data
    (update-in data [:player :items] #(dissoc % item))))

(defn add-item
  [data item n]
  ;; nil is passed in case the k is not in the list
  (-> (update-in data [:player :items item] #(+ (if % % 0) n))
      (remove-if-empty item)))

(defn add-items
  [data used-items]
  (reduce (fn [data [item n]] (add-item data item n))
          data
          used-items))


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
        (add-items {item -1})
        (u/process-event [(str "Use item: " item) [(get db/item-effects item {})]]))
    data))

(defn combine
  [data [_ used-items]]
  (let [used-items (u/filter-map (fn [[_ v]] (pos-int? v)) used-items)
        new-item (get db/recipies used-items)]
    (if (enough-items? data used-items)
      (-> data
          (add-items (u/map-map (fn [[k v]] [k (* -1 v)]) used-items))
          (add-items (if new-item
                          {new-item 1}
                          {})))
      data)))
