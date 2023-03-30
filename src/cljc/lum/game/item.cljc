(ns lum.game.item
  (:require
   [lum.game.utilities :as u]
   [lum.game.items :as items]
   [lum.game.player :as player]))

(defn enough-items?
  [data required-items]
  (items/enough? (get-in data [:player :items]) required-items))

;; High level functions
(defn equip-item
  [state [_ slot item]]
  (update state :player #(player/equip-item % slot item)))

(defn unequip-item
  [state [_ slot]]
  (update state :player #(player/unequip-item % slot)))

(defn use-item
  [data [_ item]]
  (let [{player :player msg :messages} (player/use-item (:player data) item)]
    (reduce u/add-message
            (assoc data :player player)
            msg)))

(defn combine
  [data [_ used-items]]
  (update data :player #(player/combine % used-items)))
