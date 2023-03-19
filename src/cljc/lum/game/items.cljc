(ns lum.game.items
  (:require [lum.game.utilities :as u]
            [lum.game.game-database :as db]))

(defn enough?
  [items required-items]
  (every? (fn [[k v]] (<= v (get items k 0))) required-items))

(defn clear-empty-items
  [items]
  (u/filter-map (fn [[_ v]] (pos-int? v)) items))

(defn incriedients-2-item
  [incriedients]
  (get-in db/recipies [incriedients 0]))

(defn remove-if-empty
  [items item]
  (if (pos-int? (get items item))
    items
    (dissoc items item)))

(defn add-item
  [items item n]
  ;; nil is passed in case the k is not in the list
  (-> (update items item #(+ (if % % 0) n))
      (remove-if-empty item)))

(defn add-items
  [items used-items]
  (reduce (fn [items [item n]] (add-item items item n))
          items
          used-items))

(defn combine
  [items used-items]
  (let [used-items (clear-empty-items used-items)
        new-item (incriedients-2-item used-items)]
    (if (enough? items used-items)
      (-> items
          (add-items (u/map-map (fn [[k v]] [k (* -1 v)]) used-items))
          (add-items (if new-item
                       {new-item 1}
                       {})))
      items)))
