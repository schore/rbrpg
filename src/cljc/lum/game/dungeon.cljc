(ns lum.game.dungeon
  (:require
   [clojure.math :as math]
   [lum.maputil :as mu]))

(def view-radius 10)
(def get-tile mu/get-tile)

(defn- in-view?
  [player-x player-y x y]
  (let [diffx (Math/abs (- x player-x))
        diffy (Math/abs (- y player-y))]
    (>= (* view-radius view-radius)
        (+ (* diffx diffx)
           (* diffy diffy)))))

(defn- relevant-boxes
  ([x y]
   (if (= x 0)
     (map (fn [e] [0 e]) (range 0
                                (if (pos? y) (inc y) (dec y))
                                (if (pos? y) 1 -1)))
     (let [q (/ y x)]
       (->> (range 10)
            (map #(* % (/ x 9)))
            (map (fn [i] [i (* q i)]))
            (map (fn [[x y]] [(math/round x) (math/round y)]))
            (distinct)))))
  ([x y player-x player-y]
   (->>
    (relevant-boxes (- x player-x) (- y player-y))
    (map (fn [[x y]] [(+ x player-x) (+ y player-y)])))))

(defn- visible-box?
  [board x y player-x player-y]
  (let [boxes (relevant-boxes x y player-x player-y)
        number-of-obstacles (->> boxes
                                 (map (fn [[x y]] (get-tile board x y)))
                                 (reduce (fn [a e] (if (or (> a 0)
                                                           (= (:type e) :wall))
                                                     (inc a)
                                                     a)) 0))]
    (<= number-of-obstacles 1)))

(defn update-view
  [board player-x player-y]
  (vec (for [y (range mu/sizey)
             x (range mu/sizex)]
         (if (and (in-view? player-x player-y x y)
                  (visible-box? board x y player-x player-y))
           (assoc (get-tile board x y) :visible? true)
           (get-tile board x y)))))

(defn change-tile
  [board x y f]
  (update board (mu/position-to-n x y) f))

