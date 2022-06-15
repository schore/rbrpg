(ns lum.game.cavegen
  (:require [lum.maputil :as m]
            [clojure.tools.logging :as log]))

(def xsize m/sizex)
(def ysize m/sizey)

(defn random-board []
  (into []
        (for [_ (range (* xsize ysize))]
          (if (< (rand) 0.45)
            :wall
            :ground))))

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
       (map #(if (> % 4) :wall :ground))
       (into [])))

(defn add-random-field
  [inp new-field previous-field]
  (loop []
    (let [n (rand-int (* xsize ysize))]
      (if (= previous-field (nth inp n))
        (assoc inp n new-field)
        (recur)))))

(defn get-dungeon []
  (let [f (apply comp (repeat 5 populate-map))]
    (-> (random-board)
        f
        (add-random-field :stair-up :ground)
        (add-random-field :stair-down :ground)
        m/to-map
        vec)))

(defn print-new-map
  ([] (print-new-map (get-dungeon)))
  ([m]
   (doseq [i (partition xsize (map #(case (:type %)
                                      :tree "o"
                                      :wall "#"
                                      :stair-up ">"
                                      :stair-down "<"
                                      " ") m))]
     (println i))
   (println (repeat xsize "-"))))
