(ns lum.game.game-database
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]))

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
                                "small healing potion" 1]}})

(def recipies {{"batblood" 2} "small healing potion"
               {"small healing potion" 2} "medium healing potion"
               {"batblood" 2 "batwing" 1} "small mana potion"
               {"wooden stick" 2
                "ratmeet" 2
                "herb" 1} "roast beef"
               {"wooden stick" 1
                "knife" 2} "pickaxe"
               {"knife" 1
                "wooden stick" 1} "spear"})

(def item-effects {"small healing potion" {:target :player
                                           :hp 3}
                   "small mana potion" {:target :player
                                        :mp 3}
                   "medium healing potion" {:target :player
                                            :hp 30}
                   "batblood" {}
                   "batwing" {}
                   "ratmeet" {}
                   "herb" {}
                   "roast beef" {:target :player
                                 :maxhp 1}
                   "note" {:properties #{:hint}}
                   "wooden stick" {:target :player
                                   :damage [1 4]
                                   :slots #{:right-hand}}
                   "sword" {:target :player
                            :damage [1 6]
                            :slots #{:right-hand}}
                   "knife" {:target :player
                            :damage [1 3]
                            :slots #{:right-hand}}
                   "spear" {:target :player
                            :damage [2 3]
                            :slots #{:right-hand}}
                   "pickaxe" {:target :player
                              :damage [1 4]
                              :slots #{:right-hand}
                              :properties #{:digging}}
                   "leather armor" {:target :player
                                    :ac 11
                                    :slots #{:body}}})

(def items-on-ground {"herb" {:level [1 20]
                              :dice 20}
                      "wooden stick" {:level [1 20]
                                      :dice 5}})

(def spells {"Burning Hands" {:target :enemy
                              :damage [3 6]
                              :mp 3}})

(def spell-names (map first spells))

(def hints ["Mixing batblood may help"
            "Two batblood and a wing bring happieness"
            "A spear is a knife with a long handle"
            "Two ratmeet and a herb give an excellent meal"])

(s/def ::item (into #{} (map first item-effects)))
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

;;item-effects
;;

(def slots #{:left-hand
             :right-hand
             :body})

(s/def ::slot slots)
(s/def ::slots (s/coll-of ::slot :kind set?))
(s/def ::target #{:player :enemy})
(s/def ::propertie #{:digging :burning})
(s/def ::properties (s/coll-of ::propertie
                               :kind set?))
(s/def ::effect (s/keys :req-op [::target ::hp ::hpmax ::mp ::damage ::ac ::slots ::properties]))
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
                   item-effects)))
