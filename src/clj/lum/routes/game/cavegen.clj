(ns lum.routes.game.cavegen)

(def xsize 50)
(def ysize 30)


(defn get-tile
  [input x y]
  (nth input (+ x (* y xsize))))


(defn to-map
  [col]
  (for [x (range xsize)
        y (range ysize)]
    (let [wall (get-tile col x y)]
        [[x y] wall])))


(defn random-board []
             (for [_ (range (* xsize ysize))]
               (if (< (rand) 0.45)
                 :wall
                 nil)))

(defn inmap?
  [x y]
  (and (>= x 0)
       (< x xsize)
       (>= y 0)
       (< y ysize)))

(defn count-neighbours
  [input x y]
  (->> (for [dx [-1 0 1]
             dy [-1 0 1]]
         [dx dy])
       (map (fn [[dx dy]] [(+ x dx) (+ y dy)]))
       (map (fn [[x y]] (if (inmap? x y)
                            (get-tile input x y)
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
    (into {} (-> (random-board)
                 f
                 to-map))))



(defn print-new-map []
  (doseq [i (partition xsize (map #(if (= % :wall) "#" " ")
                                  (-> (random-board)
                                      populate-map
                                      populate-map
                                      populate-map
                                      populate-map
                                      populate-map
                                      )))]
    (println i))
  (println (repeat xsize "-")))

(print-new-map)
