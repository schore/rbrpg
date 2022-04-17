(ns lum.game.game-database
  (:require [clojure.spec.alpha :as s]
            [clojure.test :as t]))

(def enemies {"Bat" {:ac 10
                     :hp 2
                     :mp 0
                     :xp 1
                     :items ["batblood" "batwing"]}
              "Rat" {:ac 3
                     :hp 1
                     :mp 0
                     :xp 1
                     :items ["ratmeet"]}})


(def recipies {{"batblood" 2} "small healing potion"
               {"small healing potion" 2} "medium healing potion"})


(def item-effects {"small healing potion" [{:target :player
                                            :stat :hp
                                            :n 3}]
                   "medium healing potion" [{:target :player
                                             :stat :hp
                                             :n 30}]
                   "batblood" []
                   "batwing" []
                   "ratmeet" []
                   "sword" []})


(s/def ::item (into #{} (map first item-effects)))
(s/def :game-database/item ::item)

;;enemie database
(s/def ::ac int?)
(s/def ::hp pos-int?)
(s/def ::mp nat-int?)
(s/def ::xp pos-int?)
(s/def ::items (s/coll-of ::item))
(s/def ::enemy (s/keys :req-un [::ac ::hp ::mp ::xp ::items]))
(s/def ::enemies (s/map-of string? ::enemy))

;;recipie database

(s/def ::recipie (s/map-of ::item pos-int?))
(s/def ::recipies (s/map-of ::recipie ::item))




;;item-effects
;;

(s/def ::target #{:player :enemy})
(s/def ::stat #{:hp :mp})
(s/def ::n int?)
(s/def ::effect (s/keys :req-un [::target ::stat ::n]))
(s/def ::effects (s/coll-of ::effect))
(s/def ::item-effects (s/map-of ::item ::effects))
