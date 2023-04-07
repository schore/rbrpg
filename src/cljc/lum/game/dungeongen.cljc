(ns lum.game.dungeongen
  (:require
   [lum.maputil :as mu]))

(def xsize mu/sizex)
(def ysize mu/sizey)

(defn empty-board
  []
  (into [] (for [_ (range (* xsize ysize))]
             :wall)))

(defn create-room
  ([m]
   (let [rx (rand-int xsize)
         ry (rand-int ysize)
         dx (+ 2 (rand-int 10))
         dy (+ 2 (rand-int 10))]
     (into []
           (for [y (range ysize)
                 x (range xsize)]
             (if (and (>= x rx) (<= x (+ rx dx))
                      (>= y ry) (<= y (+ ry dy)))
               :ground
               (mu/get-tile m x y))))))
  ([m n]
   (nth (iterate create-room m) n)))

(defn find-index
  [c f]
  (rand-nth (keep-indexed (fn [index element]
                            (when (f element) index))
                          c)))

(defn find-tile
  [board tile]
  (mu/n-to-position (find-index board #(= tile %))))

(defn create-corridor
  ([board]
   (let [[x1 y1] (find-tile board :ground)
         [x2 y2] (find-tile board :ground)]
     (into []
           (for [y (range ysize)
                 x (range xsize)]
             (if (or (and (= y y1)
                          (>= x (min x1 x2))
                          (<= x (max x1 x2)))
                     (and (= x x2)
                          (>= y (min y1 y2))
                          (<= y (max y1 y2))))
               :ground
               (mu/get-tile board x y))))))
  ([board n]
   (nth (iterate create-corridor board) n)))

(defn create-dungeon
  []
  (-> (empty-board)
      (create-room 10)
      (create-corridor 5)))

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

(print-new-map)
