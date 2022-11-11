(ns lum.game.dungeongen
  (:require [lum.maputil :as m]
            [lum.game.game-database :as db]
            [lum.maputil :as mu]))

(def xsize m/sizex)
(def ysize m/sizey)

(defn empty-board
  []
  (into [] (for [_ (range (* xsize ysize))]
             :wall)))

(defn create-room
  ([m]
   (let [rx (rand-int xsize)
         ry (rand-int ysize)
         dx (rand-int 10)
         dy (rand-int 10)]
     (into []
           (for [y (range ysize)
                 x (range xsize)]
             (if (and (>= x rx) (<= x (+ rx dx))
                      (>= y ry) (<= y (+ ry dy)))
               :ground
               (mu/get-tile m x y))))))
  ([m n]
   (nth (iterate create-room m) n)))

(defn create-dungeon
  []
  (-> (empty-board)
      (create-room 20)))

(defn print-new-map
  ([] (print-new-map (create-dungeon)))
  ([m]
   (doseq [i (partition xsize (map #(case %
                                      :tree "o"
                                      :wall "#"
                                      :stair-up ">"
                                      :stair-down "<"
                                      " ") m))]
     (println i))
   (println (repeat xsize "-"))))

;;(print-new-map)
