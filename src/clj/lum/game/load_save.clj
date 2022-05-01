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
                            \> {:type :stair-up}
                            \< {:type :stair-down}
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

(defn load-save-game
  [filename]
  (try
    (read-string (slurp  (str "tmp/" filename)))
    (catch Exception e (log/error "Exception thrown " (.getMessage e) ))))

(defn valid-save-game?
  [file-name]
  (and
   (string? file-name)
   (s/valid? :game/game (load-save-game file-name))))

(defn load-game
  [state [_ input]]
  (cond
    (s/valid? :game/game input) input
    (valid-save-game? input) (load-save-game input)
    :else state))

(defn save-game
  [state [_ filename]]
  (spit (str "tmp/" filename) state)
  state)
