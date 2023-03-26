(ns lum.game.gamelogic
  (:require
   [clojure.core.async :as a :refer [<! >! chan close! go-loop]]
   [clojure.string]
   [lum.game.dataspec]
   [lum.game.fight :as fight]
   [lum.game.item :as item]
   [lum.game.load-save :as load]
   [lum.game.magic :as magic]
   [lum.game.move :as move]
   [lum.game.view :as view]))

(def inital-items
  {"small healing potion" 2})

(defn new-board
  ([data [_ board]]
   (-> data
       (assoc-in [:boards (dec (:level data))] board)
       (move/set-to-tile :stair-up))))

(defn initialize
  [_ [_ board]]
  (-> {:boards [board]
       :level 1
       :messages '("Hello adventurer"
                   ""
                   "Welcome to my dungeon"
                   "" ""
                   "You will have a lot of fun"
                   "T

he madness will go into your soul"
                   "" ""
                   "Try not to die")
       :player {:position [10 10]
                :ac 5
                :xp 0
                :hp [10 10]
                :mp [3 3]
                :equipment {}
                :recepies []
                :spells #{"Burning Hands" "Healing"}
                :items inital-items}
       :coeffects []}
      (move/set-to-tile :stair-up)))

(defn game-over?
  [data]
  (= 0 (get-in data [:player :hp 0])))

(def basic-mode
  {:initialize [initialize]
   :load [load/load-game]
   :enter-unknown-level [move/enter-unknown-level]
   :save [#?(:clj load/save-game)]
   :equip [item/equip-item]
   :unequip [item/unequip-item]
   :nop []})

(def game-over-mode
  (merge basic-mode
         {}))

(def fight-mode
  (merge basic-mode
         {:attack [fight/attack fight/check-fight-end]
          :cast-spell [magic/cast-spell-during-fight fight/check-fight-end]
          :flea [fight/flea]
          :use-item [item/use-item]}))

(def move-mode
  (merge basic-mode
         {:activate [move/activate fight/check-fight]
          :cast-spell [magic/cast-spell]
          :load-map [load/load-map]
          :move [move/move fight/check-fight]
          :new-board [new-board]
          :combine [item/remember-recipies item/combine]
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

(defn process-round
  [data action]
  (-> data
      (process-actions action)
      view/process-view))

(defn calc-commands
  [data action]
  (let [new-data (process-round data action)]
    {:data (assoc new-data :coeffects [])
     :events (if (empty? (:coeffects new-data))
               [[:new-state new-data]]
               (:coeffects new-data))}))

(defn game-logic
  [input-chan]
  (let [out (chan)]
    (go-loop [data {}]
      (if-let [action (<! input-chan)]
        (let [{new-data :data
               events :events} (calc-commands data action)]
          (doseq [event events] (>! out event))
          (recur new-data))
        (close! out)))
    out))

(defn load-special-map
  [map level]
  (-> map
      (load/load-map-from-string)
      (move/load-effect level)))
