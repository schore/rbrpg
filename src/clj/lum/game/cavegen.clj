(ns lum.game.cavegen
  (:require [lum.maputil :as m]))

(def xsize m/sizex)
(def ysize m/sizey)

(defn random-board []
  (for [_ (range (* xsize ysize))]
    (if (< (rand) 0.45)
      :wall
      nil)))

(defn count-neighbours
  [input x y]
  (->> (for [dx [-1 0 1]
             dy [-1 0 1]]
         [dx dy])
       (map (fn [[dx dy]] [(+ x dx) (+ y dy)]))
       (map (fn [[x y]] (if (m/inmap? x y)
                          (m/get-tile input x y)
                          :wall)))
       (filter #(= % :wall))
       count))

(defn map-to-count
  [input]
  (for [y (range ysize)
        x (range xsize)]
    (count-neighbours input x y)))

(defn populate-map
  [input]
   (->> (map-to-count input)
        (map #(if (> % 4) :wall nil))))

(defn get-dungeon []
  (let [f (apply comp (repeat 5 populate-map))]
    (-> (random-board)
        f
        m/to-map)))

(defn print-new-map
  ([] (print-new-map (get-dungeon)))
  ([m]
   (doseq [i (partition xsize (map #(if (= (:type %) :wall) "#" " ") m))]
     (println i))
   (println (repeat xsize "-"))))

