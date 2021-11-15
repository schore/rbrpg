(ns lum.game.dataspec
  (:require [clojure.spec.alpha :as s]
            [clojure.walk]
            [lum.game.cavegen :as g]))

(s/def :tile/type (s/nilable #{:wall
                               :tree}))

(s/def :game/tile (s/keys :req-un [:tile/type]))

(defn valid-tile? [tile] (s/valid? :game/tile tile))

(s/def :game/dungeon (s/coll-of :game/tile
                                :count 1500))



(s/valid? :game/dungeon (repeat 1500 {:type :wall}))

(s/explain :game/dungeon (g/get-dungeon))

;; (time (let [data (into [] (g/get-dungeon))]
;;         (time (g/print-new-map data))
;;         ))


(g/get-dungeon)
