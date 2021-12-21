(ns lum.game.dataspec
  (:require [clojure.spec.alpha :as s]
            [lum.maputil :as mu]
            [lum.game.cavegen :as g]))

(s/def :tile/type  #{:wall
                     :ground
                     :tree})

(s/def :game/tile (s/keys :req-un [:tile/type]))

(s/def :game/board (s/coll-of :game/tile
                              :count 1500))

(s/def :game/position (s/cat :x (fn [x] (and (nat-int? x)
                                             (< x mu/sizex)))
                             :y (fn [y] (and (nat-int? y)
                                             (< y mu/sizey)))))

(s/def :player/xp nat-int?)

(s/def :game/stat (s/and (s/cat :current nat-int? :max nat-int?)
                           #(<= (:current %) (:max %))))

(s/def :player/hp :game/stat)
(s/def :player/mp :game/stat)

(s/def :game/player (s/keys :req-un [:game/position
                                     :player/xp
                                     :player/hp
                                     :player/mp]))

(s/def :enemy/name string?)
(s/def :enemy/hp :game/stat)
(s/def :enemy/mp :game/stat)

(s/def :fight/enemy (s/keys :req-un [:enemy/name
                                     :enemy/hp
                                     :enemy/mp]))



(s/def :effect/target #{:player :enemy})
(s/def :effect/stat #{:hp :mp})
(s/def :effect/n int?)

(s/def :fight/action (s/cat :message string?
                            :effects (s/coll-of (s/keys :req-un [:effect/target
                                                                 :effect/stat
                                                                 :effect/n]))))

(s/def :fight/actions (s/coll-of :fight/action))

(s/def :game/fight (s/keys :req-un [:fight/enemy
                                    :fight/actions]))


(s/def :game/game (s/keys :req-un [:game/player
                                   :game/board]
                          :opt-un [:game/fight]))





(s/explain :game/game {:board (g/get-dungeon)
                       :player {:position [1 1]
                                :xp 0
                                :hp [8 10]
                                :mp [0 3]}
                       :fight {:enemy {:hp [20 20]
                                       :mp [0 0]}
                               :actions [["Heal" [{:target :player
                                                   :stat :hp
                                                   :n 5}]]]}})
