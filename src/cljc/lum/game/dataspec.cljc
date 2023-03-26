(ns lum.game.dataspec
  (:require [clojure.spec.alpha :as s]
            [lum.maputil :as mu]
            ;[lum.game.cavegen :as g]
            [lum.game.game-database :as db]
            ;[clojure.tools.logging :as log]
            ))

(s/def :tile/type  #{:wall
                     :ground
                     :stair-up
                     :stair-down
                     :tree})

(s/def :tile/visible? boolean?)
(s/def :tile/message string?)

(s/def :enemy/name (into #{} db/enemy-names))

(s/def :tile/enemy :enemy/name)

(s/def :game/items (s/map-of :game-database/item pos-int?))

(s/def :game/tile (s/keys :req-un [:tile/type :tile/visible?]
                          :opt-un [:game/items
                                   :tile/message
                                   :tile/enemy]))

(defn board-contains-element?
  [element board]
  (< 0 (count (filter #(= element (:type %))
                      (s/unform :game/board board)))))

(s/def :game/board (s/and (s/coll-of :game/tile
                                     :count 1500)
                          (partial board-contains-element? :stair-up)
                          (partial board-contains-element? :stair-down)))

(s/def :game/boards (s/coll-of :game/board))
(s/def :game/level pos-int?)

(s/def :game/position (s/cat :x (s/and nat-int?
                                       #(< % mu/sizex))
                             :y (s/and nat-int?
                                       #(< % mu/sizey))))

(s/def :player/xp nat-int?)

(s/def :game/stat (s/and (s/cat :current nat-int? :max nat-int?)
                         #(<= (:current %) (:max %))))

(s/def :player/hp :game/stat)
(s/def :player/mp :game/stat)
(s/def :player/ac pos-int?)

(s/def ::slot db/slots)
(s/def :player/equipment (s/map-of ::slot :game-database/item))

(s/def :player/spell (into #{} db/spell-names))
(s/def :player/spells (s/and (s/coll-of :player/spell)
                             set?))

(def recepie (into #{}
                   (map first db/recipies)))

(s/def :game/recepie recepie)
(s/def :game/recepies (s/coll-of :game/recepie))

(s/def :game/player (s/keys :req-un [:game/position
                                     :player/ac
                                     :player/xp
                                     :player/hp
                                     :player/mp
                                     :player/equipment
                                     :player/spells
                                     :game/items
                                     :game/recepies]))

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
        tile (:type (mu/get-tile (get-in data [:boards (dec (:level data))]) x y))]
    (contains? #{:ground
                 :stair-down
                 :stair-up}
               tile)))

(defn valid-level?
  [data]
  (>= (count (:boards data)) (:level data)))

(s/def :game/game (s/and (s/keys :req-un [:game/player
                                          :game/boards
                                          :game/level
                                          :game/messages]
                                 :opt-un [:game/fight])
                         valid-position?
                         valid-level?))

;; (s/explain :game/game {:boards [(g/get-dungeon)]
;;                        :level 1
;;                        :player {:position [10 10]
;;                                 :xp 0
;;                                 :ac 10
;;                                 :hp [8 10]
;;                                 :mp [0 3]
;;                                 :spells #{"Burning Hands"}
;;                                 :equipment {:right-hand "sword"}
;;                                 :items {"batwing" 2
;;                                         "batblood" 2}}
;;                        :messages ["Hello World"]
;;                        :fight {:enemy {:name "bat"
;;                                        :ac 1
;;                                        :hp [20 20]
;;                                        :mp [0 0]}
;;                                :actions [["Heal" [{:target :player
;;                                                    :stat :hp
;;                                                    :n 5}]]]}})
