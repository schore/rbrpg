(ns lum.game.load-save
  (:require [clojure.spec.alpha :as s]
            [clojure.string]
            [lum.maputil :as mu]
            [lum.game.dataspec]
            [lum.game.move :as move]))

(defn pad [n pad coll]
  (take n (concat coll (repeat pad))))

(defn load-map-from-string
  [inp]
  (vec
   (->> (clojure.string/split-lines inp)
        (map (fn [line]
               (->> (seq line)
                    (map (fn [c]
                           (case c
                             \  {:type :ground}
                             \. {:type :ground}
                             \> {:type :stair-up}
                             \< {:type :stair-down}
                             \# {:type :wall}
                             {:type :wall}))))))
        (map (fn [line]
               (pad mu/sizex {:type :wall} line)))
        flatten
        (pad (* mu/sizex mu/sizey) {:type :wall})
        (map #(assoc % :visible? false)))))

(defn load-game
  [state [_ input]]
  (cond
    (s/valid? :game/game input) input
    :else state))
