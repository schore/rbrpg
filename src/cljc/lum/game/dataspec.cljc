(ns lum.game.dataspec
  (:require [clojure.spec.alpha :as s]
            [lum.maputil :as mu]
            ;[lum.game.cavegen :as g]
            [lum.game.game-database :as db]))

(s/def :tile/type  #{:wall
                     :ground
                     :stair-up
                     :stair-down
                     :tree})

(s/def :game/tile (s/keys :req-un [:tile/type]))

(defn board-contains-element?
  [element board]
  (< 0 (count (filter #(= element (:type %))
                      (s/unform :game/board board)))))


(s/def :game/board (s/and (s/coll-of :game/tile
                                     :count 1500)
                          (partial board-contains-element? :stair-up)
                          (partial board-contains-element? :stair-down)))

(s/def :game/position (s/cat :x (fn [x] (and (nat-int? x)
                                             (< x mu/sizex)))
                             :y (fn [y] (and (nat-int? y)
                                             (< y mu/sizey)))))

(s/def :player/xp nat-int?)



(s/def :game/stat (s/and (s/cat :current nat-int? :max nat-int?)
                           #(<= (:current %) (:max %))))

(s/def :player/hp :game/stat)
(s/def :player/mp :game/stat)
(s/def :player/ac pos-int?)

(s/def ::slot db/slots)
(s/def :player/equipment (s/map-of ::slot :game-database/item))

(s/def :game/player (s/keys :req-un [:game/position
                                     :player/ac
                                     :player/xp
                                     :player/hp
                                     :player/mp
                                     :player/equipment
                                     :game-database/items]))

(s/def :enemy/name string?)
(s/def :enemy/hp :game/stat)
(s/def :enemy/mp :game/stat)
(s/def :enemy/ac pos-int?)

(s/def :fight/enemy (s/keys :req-un [:enemy/name
                                     :enemy/hp
                                     :enemy/mp
                                     :enemy/ac]))



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

(s/def :game/messages (s/coll-of string?
                                 :max-count 10))

(defn valid-position?
  [data]
  (let [data (s/unform :game/game data)
        [x y] (get-in data [:player :position])
        tile (:type (mu/get-tile (:board data) x y))]
    (contains? #{:ground
                 :stair-down
                 :stair-up}
               tile)))

(s/def :game/game (s/and (s/keys :req-un [:game/player
                                          :game/board
                                          :game/messages]
                                 :opt-un [:game/fight]
                                 )
                         valid-position?))



;; (s/explain :game/game {:board (g/get-dungeon)
;;                        :player {:position [10 10]
;;                                 :xp 0
;;                                 :ac 10
;;                                 :hp [8 10]
;;                                 :mp [0 3]
;;                                 :equipment {:right-hand "sword"}
;;                                 :items {"batwing" 2
;;                                         "batblood" 2}}
;;                        :messages ["Hello World"]
;;                        :fight {:enemy {
;;                                        :name "bat"
;;                                        :ac 1
;;                                        :hp [20 20]
;;                                        :mp [0 0]}
;;                                :actions [["Heal" [{:target :player
;;                                                    :stat :hp
;;                                                    :n 5}]]]}})
