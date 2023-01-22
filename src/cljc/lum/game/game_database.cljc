(ns lum.game.game-database
  (:require [clojure.spec.alpha :as s]
            [lum.game.load-save :as load]))

(def enemies {"Bat" {:ac 10
                     :damage [1 3]
                     :hp 2
                     :mp 0
                     :xp 1
                     :items ["batblood" 1 "batwing" 1]}
              "Rat" {:ac 3
                     :damage [1 2]
                     :level [0 10]
                     :hp 1
                     :mp 0
                     :xp 1
                     :items ["ratmeet" 1]}
              "Crazy" {:ac 7
                       :damage [3 2]
                       :level [2 20]
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
              "Imp" {:ac 10
                     :damage [2 3]
                     :level [5 0]
                     :hp 10
                     :mp 0
                     :xp 10
                     :items ["small mana potion" 10
                             "small mana potion" 10
                             "medium mana potion" 15
                             "medium healing potion" 18]}
              "Deamon" {:ac 13
                        :damage [2 6]
                        :level [5 0]
                        :hp 20
                        :mp 0
                        :xp 20
                        :items ["fire sword" 15
                                "ore" 5]}})

(def recipies {{"batblood" 2} ["small healing potion" "Mixing batblood may help"]
               {"small healing potion" 2} ["medium healing potion" "More is stronger"]
               {"small mana potion" 2} ["medium mana potion" "Two are better than one"]
               {"batblood" 2 "batwing" 1} ["small mana potion" "May the spirtit be with you"]
               {"small healing potion" 1
                "batwing" 1} ["small mana potion" "A wing bringth the spirit"]
               {"wooden stick" 2
                "ratmeet" 2
                "herb" 1} ["roast beef" "Ratmeat and a herb"]
               {"wooden stick" 1
                "knife" 2} ["pickaxe"  "Two knife on a wooden stick help to dig"]
               {"knife" 1
                "wooden stick" 1} ["spear" "A spear is a knife with a long handle"]
               {"leather armor" 2} ["enhanced leather armor" "How to make a great leather armor"]
               {"enhanced leather armor" 1
                "ore" 10} ["scale mail" "Heavy metal protects"]
               {"wooden stick" 10} ["wooden shield" "Protect yourself with wood"]})

(def item-data {"small healing potion" {:target :player
                                        :hp 3
                                        :rarity 5}
                "small mana potion" {:target :player
                                     :mp 3
                                     :rarity 5}
                "medium mana potion" {:target :player
                                      :mp 30
                                      :rarity 2}
                "medium healing potion" {:target :player
                                         :hp 30
                                         :rarity 3}
                "batblood" {}
                "batwing" {}
                "ore" {}
                "ratmeet" {:rarity 7}
                "herb" {:rarity 3}
                "roast beef" {:target :player
                              :maxhp 1}
                "note" {:properties #{:hint}
                        :rarity 4}
                "force scroll" {:spell "Force"
                                :rarity 1}
                "magic mapping scroll" {:spell "Magic Mapping"
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
                "wooden shield" {:target :player
                                 :ac 2
                                 :slots #{:left-hand}}
                "leather armor" {:target :player
                                 :ac 11
                                 :slots #{:body}
                                 :rarity 4}
                "enhanced leather armor" {:target :player
                                          :ac 12
                                          :slots #{:body}
                                          :rarity 2}
                "scale mail" {:target :player
                              :ac 14
                              :slots #{:body}}})

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

(def hints (map (fn [[_ [_ m]]] m) recipies))

(def special-maps
  {5 {:map (load/static-load-file "resources/docs/test.txt")
      :effects [[11 10] :message "A board squeezes"
                [0 0] :message "Bla"
                [11 11] :enemy "Bandit"
                [15 15] :item "magic mapping scroll"
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
(s/def ::recipies (s/map-of ::recipie (s/cat :item ::item
                                             :message string?)))

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
