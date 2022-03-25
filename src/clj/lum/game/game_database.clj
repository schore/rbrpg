(ns lum.game.game-database
  (:require [clojure.spec.alpha :as s]
            [lum.game.dataspec]
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


(def recipies {{"batblood" 2} "healing potion"})


;;enemie database
(s/def ::ac int?)
(s/def ::hp pos-int?)
(s/def ::mp nat-int?)
(s/def ::xp pos-int?)
(s/def ::items (s/coll-of :game/item))
(s/def ::enemy (s/keys :req-un [::ac ::hp ::mp ::xp ::items]))
(s/def ::enemies (s/map-of string? ::enemy))

;;recipie database

(s/def ::recipie (s/map-of :game/item pos-int?))
(s/def ::recipies (s/map-of ::recipie :game/item))


(t/deftest valid-enemy-database
  (t/is (s/valid? ::enemies enemies)))

(t/deftest valid-recipie-database
  (t/is (s/valid? ::recipies recipies)))
