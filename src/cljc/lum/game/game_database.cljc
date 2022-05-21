(ns lum.game.game-database
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]))

(def enemies {"Bat" {:ac 10
                     :damage [1 3]
                     :hp 2
                     :mp 0
                     :xp 1
                     :items ["batblood" "batwing"]}
              "Rat" {:ac 3
                     :damage [1 2]
                     :hp 1
                     :mp 0
                     :xp 1
                     :items ["ratmeet"]}})


(def recipies {{"batblood" 2} "small healing potion"
               {"small healing potion" 2} "medium healing potion"
               {"sword" 1
                "wooden stick" 1} "pickaxe"})


(def item-effects {"small healing potion" [{:target :player
                                            :hp 3}]
                   "medium healing potion" [{:target :player
                                             :hp 30}]
                   "batblood" []
                   "batwing" []
                   "ratmeet" []
                   "herb" []
                   "wooden stick" [{:target :player
                                    :damage [1 4]
                                    :slots #{:right-hand}}]
                   "sword" [{:target :player
                             :damage [1 6]
                             :slots #{:right-hand}}]
                   "pickaxe" [{:target :player
                               :damage [1 4]
                               :slot #{:right-hand}
                               :properties #{:digging}}]
                   "leather armor" [{:target :player
                                     :ac 11
                                     :slots #{:body}}]})


(s/def ::item (into #{} (map first item-effects)))
(s/def :game-database/item ::item)

;;enemie database
(s/def ::ac int?)
(s/def ::hp int?)
(s/def ::mp int?)
(s/def ::xp pos-int?)
(s/def ::damage (s/cat :n pos-int? :faces pos-int?))
(s/def ::items (s/coll-of ::item))
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
(s/def ::effect (s/keys :req-un [::target]
                        :req-op [::hp ::mp ::damage ::ac ::slots ::properties]))

(s/def ::effects (s/coll-of ::effect))
(s/def ::item-effects (s/map-of ::item ::effects))


(defn get-items-for-slot
  [slot]
  (map key (filter (fn [[_ v]] (contains? (:slots (first v)) slot ))
                   item-effects)))
