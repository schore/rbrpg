(ns lum.game.game-database
  (:require [clojure.spec.alpha :as s]
            [lum.game.load-save :as load]
            [clojure.string :as string]))

(def enemies {"Bat" {:ac 10
                     :damage [1 3]
                     :hp 2
                     :mp 0
                     :xp 1
                     :items ["batblood" 1 "batwing" 1]}
              "Rat" {:ac 3
                     :damage [1 2]
                     :hp 1
                     :mp 0
                     :xp 1
                     :items ["ratmeet" 1]}
              "Crazy" {:ac 7
                       :damage [3 2]
                       :hp 4
                       :mp 0
                       :xp 2
                       :items ["knife" 10
                               "note" 10
                               "small healing potion" 3
                               "small healing potion" 3]}
              "Bandit" {:ac 7
                        :damage [1 6]
                        :hp 5
                        :mp 0
                        :xp 3
                        :items ["leather armor" 15
                                "note" 17
                                "medium healing potion" 5
                                "small healing potion" 1]}
              "Deamon" {:ac 13
                        :damage [2 6]
                        :level 5
                        :hp 20
                        :mp 0
                        :xp 20
                        :items ["fire sword" 1]}})

(def recipies {{"batblood" 2} "small healing potion"
               {"small healing potion" 2} "medium healing potion"
               {"batblood" 2 "batwing" 1} "small mana potion"
               {"small healing potion" 1
                "batwing" 1} "small mana potion"
               {"wooden stick" 2
                "ratmeet" 2
                "herb" 1} "roast beef"
               {"wooden stick" 1
                "knife" 2} "pickaxe"
               {"knife" 1
                "wooden stick" 1} "spear"})

(def item-data {"small healing potion" {:target :player
                                        :hp 3
                                        :rarity 5}
                "small mana potion" {:target :player
                                     :mp 3
                                     :rarity 5}
                "medium healing potion" {:target :player
                                         :hp 30
                                         :rarity 3}
                "batblood" {}
                "batwing" {}
                "ratmeet" {:rarity 7}
                "herb" {:rarity 3}
                "roast beef" {:target :player
                              :maxhp 1}
                "note" {:properties #{:hint}
                        :rarity 4}
                "Force scroll" {:spell "Force"
                                :rarity 1}
                "Magic Mapping scroll" {:spell "Magic Mapping"
                                        :rarity 1}
                "wooden stick" {:target :player
                                :damage [1 4]
                                :slots #{:right-hand}
                                :rarity 10}
                "sword" {:target :player
                         :damage [1 6]
                         :slots #{:right-hand}
                         :rarity 1}
                "fire sword" {:target :player
                              :damage [2 6]
                              :slots #{:right-hand}}
                "knife" {:target :player
                         :damage [1 3]
                         :slots #{:right-hand}
                         :rarity 4}
                "spear" {:target :player
                         :damage [2 3]
                         :slots #{:right-hand}
                         :rarity 4}
                "pickaxe" {:target :player
                           :damage [1 4]
                           :slots #{:right-hand}
                           :properties #{:digging}
                           :rarity 4}
                "leather armor" {:target :player
                                 :ac 11
                                 :slots #{:body}
                                 :rarity 4}})

(def itemlist
  (->> item-data
       (filter #(contains? (second %) :rarity))
       (map (fn [[k v]] (repeat (:rarity v) k)))
       flatten))

(def items-on-ground {"herb" {:level [1 20]
                              :dice 20}
                      "wooden stick" {:level [1 20]
                                      :dice 5}})

(def spells {"Burning Hands" {:target :enemy
                              :damage [3 6]
                              :mp 3}
             "Force" {:target :enemy
                      :damage [3 3]
                      :mp 1}
             "Healing" {:target :player
                        :hp 5
                        :mp 1}
             "Magic Mapping" {:target :player
                              :mp 3
                              :mapping? true}})

(def spell-names (map first spells))
(def enemy-names (map first enemies))

(def hints ["Mixing batblood may help"
            "Life and wings let you fly"
            "Two batblood and a wing bring happieness"
            "A spear is a knife with a long handle"
            "Two ratmeet and a herb give an excellent meal"])

(def special-maps
  {5 {:map (load/static-load-file "resources/docs/test.txt")
      :effects [[11 10] :message "A board squeezes"
                [0 0] :message "Bla"
                [11 11] :enemy "Bandit"
                [15 15] :item "Magic Mapping scroll"
                [20 24] :enemy "Deamon"]}})

(s/def ::item (into #{} (map first item-data)))
(s/def :game-database/item ::item)

;;enemie database
(s/def ::ac int?)
(s/def ::hp int?)
(s/def ::hpmax int?)
(s/def ::mp int?)
(s/def ::xp pos-int?)
(s/def ::damage (s/cat :n pos-int? :faces pos-int?))
(s/def ::items (s/* (s/cat :item ::item :dice pos-int?)))
(s/def ::enemy (s/keys :req-un [::ac ::damage ::hp ::mp ::xp ::items]))
(s/def ::enemies (s/map-of string? ::enemy))

;;recipie database

(s/def ::recipie (s/map-of ::item pos-int?))
(s/def ::recipies (s/map-of ::recipie ::item))

;;item-data
;;

(def slots #{:left-hand
             :right-hand
             :body})

(s/def ::rarity nat-int?)

(s/def ::slot slots)
(s/def ::slots (s/coll-of ::slot :kind set?))
(s/def ::target #{:player :enemy})
(s/def ::propertie #{:digging :burning :hint})
(s/def ::properties (s/coll-of ::propertie
                               :kind set?))
(s/def ::spell (into #{} spell-names))
(s/def ::effect (s/keys
                 :opt-un [::target
                          ::rarity
                          ::hp
                          ::hpmax
                          ::mp
                          ::damage
                          ::ac
                          ::slots
                          ::properties
                          ::spell]))
(s/def ::item-effects (s/map-of ::item ::effect))

;; Item on grounds
;;

(s/def ::level (s/and (s/cat :from pos-int? :to pos-int?)
                      (fn [{:keys [from to]}]
                        (<= from to))))
(s/def ::dice (s/and pos-int?
                     #(<= % 20)))
(s/def ::ground-item (s/keys :req-un [::level ::dice]))
(s/def ::ground-items (s/map-of ::item ::ground-item))

;;spells

(s/def ::spells (s/map-of string? ::effect))

(defn get-items-for-slot
  [slot]
  (map key (filter (fn [[_ v]] (contains? (:slots v) slot))
                   item-data)))

;; special maps
;;
;;

(s/def ::enemies-name (into #{} (map first enemies)))

(s/def ::map string?)
(s/def ::map-tile (s/cat :x nat-int? :y nat-int?))

(s/def ::message-effect (s/cat :type #{:message} :string string?))
(s/def ::fight-effect (s/cat :type #{:enemy} :enemy ::enemies-name))
(s/def ::item-effect (s/cat :type #{:item} :item ::item))
(s/def ::effect-types (s/alt :message ::message-effect
                             :fight ::fight-effect
                             :item ::item-effect))

(s/def ::effects (s/* (s/cat :tile (s/spec ::map-tile)
                             :effect ::effect-types)))
(s/def ::special-map (s/keys :req-un [::map ::effects]))

(s/def ::map-db (s/map-of nat-int? ::special-map))
