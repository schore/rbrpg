(ns lum.game.cavegen
  (:require [lum.maputil :as m]
            [lum.game.game-database :as db]))

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

(defn find-random-tile
  [input type]
  (let [i (rand-int (* xsize ysize))
        field (get-in input [i :type])]
    (if (= type field)
      i
      (recur input type))))

(defn add-item-on-random-field
  ([input n]
   (if (= 0 n)
     input
     (recur (add-item-on-random-field input 1 :ground {(rand-nth db/itemlist) 1}) (dec n))))
  ([input n items]
   (if (= 0 n)
     input
     (recur (assoc-in input [(rand-int (* xsize ysize)) :items] items) (dec n) items)))
  ([input n field items]
   (if (= 0 n)
     input
     (recur
      (let [i (find-random-tile input field)]
        (assoc-in input [i :items] items))
      (dec n)
      field
      items))))

(defn add-npc-on-random-tile
  [input]
  (let [i (find-random-tile input :ground)]
    (assoc-in input [i :npc] [["Hello World"]])))

(defn place-items
  [state]
  (-> state
      (add-item-on-random-field 5 :ground {"wooden stick" 1})
      (add-item-on-random-field 5)))

(defn get-dungeon []
  (let [f (apply comp (repeat 5 populate-map))]
    (-> (random-board)
        f
        (add-random-field :stair-up :ground)
        (add-random-field :stair-down :ground)
        m/to-map
        vec
        place-items
        add-npc-on-random-tile)))

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
