(ns lum.game.gamelogic
  (:require
   [clojure.core.async :as a :refer [<! >! chan close! go-loop]]
   [clojure.string]
   [lum.game.cavegen :as cavegen]
   [lum.game.load-save :as load-save]
   [lum.game.dataspec]
   [lum.game.update-data]
   [lum.game.item :as item]
   [lum.game.move :as move]
   [lum.game.fight :as fight]
   [lum.game.magic :as magic]))

(def inital-items
  {"small healing potion" 2})

(defn new-board
  [data _]
  (-> data
      (assoc-in [:boards (dec (:level data))] (cavegen/get-dungeon))
      (move/set-to-tile :ground)))

(defn initialize
  [_ _]
  (-> {:boards [(cavegen/get-dungeon)]
       :level 1
       :messages '("Hello adventurer"
                   ""
                   "Welcome to my dungeon"
                   "" ""
                   "You will have a lot of fun"
                   "The madness will go into your soul"
                   "" ""
                   "Try not to die")
       :player {:position [10 10]
                :ac 5
                :xp 0
                :hp [10 10]
                :mp [3 3]
                :equipment {}
                :spells #{"Burning Hands"}
                :items inital-items}}
      (move/set-to-tile :ground)))

(defn game-over?
  [data]
  (= 0 (get-in data [:player :hp 0])))

(def basic-mode
  {:initialize [initialize]
   :load [load-save/load-game]
   :save [#?(:clj load-save/save-game)]
   :equip [item/equip-item]
   :unequip [item/unequip-item]
   :nop []})

(def game-over-mode
  (merge basic-mode
         {}))

(def fight-mode
  (merge basic-mode
         {:attack [fight/attack fight/check-fight-end]
          :cast-spell [magic/cast-spell fight/check-fight-end]
          :flea [fight/flea]
          :use-item [item/use-item]}))

(def move-mode
  (merge basic-mode
         {:activate [move/activate fight/check-fight]
          :cast-spell [magic/cast-spell]
          :load-map [#?(:clj load-save/load-map)]
          :move [move/move fight/check-fight]
          :set-position [move/set-position]
          :new-board [new-board]
          :combine [item/combine]
          :use-item [item/use-item]}))

(defn get-mode-map
  [state]
  (cond
    (game-over? state) game-over-mode
    (contains? state :fight) fight-mode
    :else move-mode))

(defn process-actions
  [data action]
  (when (nil? (get (get-mode-map data) (keyword (first action))))
    (println "No entry defined " action))
  (reduce (fn [data f]
            (f data action))
          data (get (get-mode-map data)
                    (keyword (first action)) [])))

(defn game-master
  [input-chan]
  (let [out (chan)]
    (go-loop [data {}]
      (let [action (<! input-chan)
            new-data (-> data
                         (process-actions action))]
        (if (some? action)
          (do
            (>! out new-data)
            (recur new-data))
          (do
            (close! out)
            data))))
    out))
