(ns lum.game.view
  (:require
   [clojure.spec.alpha :as s]
   [clojure.math :as math]
   [lum.maputil :as mu]
   [lum.game.dataspec]))

(def view-radius 10)

(defn in-view?
  [player-x player-y x y]
  (let [diffx (Math/abs (- x player-x))
        diffy (Math/abs (- y player-y))]
    (>= (* view-radius view-radius)
        (+ (* diffx diffx)
           (* diffy diffy)))))

(defn relevant-boxes
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

(defn visible-box?
  [board x y player-x player-y]
  (let [boxes (relevant-boxes x y player-x player-y)
        number-of-obstacles (->> boxes
                                 (map (fn [[x y]] (mu/get-tile board x y)))
                                 (reduce (fn [a e] (if (or (> a 0)
                                                           (= (:type e) :wall))
                                                     (inc a)
                                                     a)) 0))]
    (<= number-of-obstacles 1)))

(defn update-board
  [board player-x player-y]
  (vec (for [y (range mu/sizey)
             x (range mu/sizex)]
         (if (and (in-view? player-x player-y x y)
                  (visible-box? board x y player-x player-y))
           (assoc (mu/get-tile board x y) :visible? true)
           (mu/get-tile board x y)))))

(defn update-data
  [data]
  (let [[x y] (get-in data [:player :position])]
    (update-in data [:boards (dec (:level data))]
               (fn [board]
                 (update-board board x y)))))

(defn process-view
  [data]
  (if (s/valid? :game/game data)
    (-> data
        update-data)
    data))
