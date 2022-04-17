(ns lum.game.load-save
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clojure.string]
            [clojure.java.io :as io]
            [lum.maputil :as mu]
            [lum.game.dataspec]))

(defn pad [n pad coll]
  (take n (concat coll (repeat pad))))

(defn load-map-from-string
  [inp]
  (->> (clojure.string/split-lines inp)
       (map (fn [line]
              (->> (seq line)
                   (map (fn [c]
                          (case c
                            \  {:type :ground}
                            \. {:type :ground}
                            \# {:type :wall}
                            {:type :wall}))))))
       (map (fn [line]
              (pad mu/sizex {:type :wall} line)))
       flatten
       (pad (* mu/sizex mu/sizey) {:type :wall})))


(defn load-map
  [data [_ file]]
  (if-let [mf (try
                (slurp (io/resource file))
                (catch Exception e (log/error "Exception thrown " (.getMessage e))))]
    (assoc data :board (load-map-from-string mf))
    data))


(defn load-game
  [state [_ new-data]]
  (if (s/valid? :game/game new-data)
    new-data
    state))
