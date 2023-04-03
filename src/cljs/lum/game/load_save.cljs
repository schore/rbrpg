(ns lum.game.load-save
  (:require [clojure.spec.alpha :as s]
            [clojure.string]
            [lum.maputil :as mu])
  (:require-macros [lum.game.load-save :as m]))

(def testmap
  (m/static-load-file "resources/docs/test.txt"))

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

(defn load-map
  [data [_ file]]
  (let [level (get-in data [:board :player-position 0])]
    (if-let [mf testmap]
      (-> data
          (assoc-in [:board :dungeons (dec level)] (load-map-from-string mf))
          (assoc-in [:board :player-position] [level 10 10]))
      data)))

(defn load-game
  [state [_ input]]
  (cond
    (s/valid? :game/game input) input
    :else state))
