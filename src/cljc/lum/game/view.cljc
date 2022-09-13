(ns lum.game.view
  (:require
   [clojure.spec.alpha :as s]
;;   [clojure.math :as math]
   [lum.maputil :as mu]
   [lum.game.dataspec]))

(def view-radius 5)

(defn in-view?
  [player-x player-y x y]
  (let [diffx (Math/abs (- x player-x))
        diffy (Math/abs (- y player-y))]
    (>= (* view-radius view-radius)
        (+ (* diffx diffx)
           (* diffy diffy)))))

;; (defn relevant-boxes
;;   [x y]
;;   (let [q (/ y x)]
;;     (->> (range 10)
;;          (map #(* % (/ x 9)))
;;          (map (fn [i] [i (* q i)]))
;;          (map (fn [[x y]] [(math/round x) (math/round y)])))))

(defn update-board
  [board player-x player-y]
  (vec (for [y (range mu/sizey)
             x (range mu/sizex)]
         (if (in-view? player-x player-y x y)
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
