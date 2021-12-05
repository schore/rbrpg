(ns lum.game.dataspec
  (:require [clojure.spec.alpha :as s]
            [lum.maputil :as mu]
            [lum.game.cavegen :as g]))

(s/def :tile/type (s/nilable #{:wall
                               :tree}))

(s/def :game/tile (s/keys :req-un [:tile/type]))


(s/def :game/board (s/coll-of :game/tile
                                :count 1500))


(s/def :game/position (s/cat :x (fn [x] (and (nat-int? x)
                                             (< x mu/sizex)))
                             :y (fn [y] (and (nat-int? y)
                                             (< y mu/sizey)))))

(s/def :game/player (s/keys :req-un [:game/position]))

(s/def :npc/type #{:elf
                   :monster})

(s/def :game/npc (s/keys :req-un [:game/position
                                  :npc/type]))

(s/def :game/npcs (s/coll-of :game/npc))


(s/def :game/game (s/keys :req-un [:game/npcs
                                   :game/player
                                   :game/board]))

;; (s/valid? :game/game {:board (g/get-dungeon)
;;                        :player {:position [1 1]}
;;                        :npcs  []})
