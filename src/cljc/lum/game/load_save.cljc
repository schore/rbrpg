(ns lum.game.load-save
  (:require [clojure.spec.alpha :as s]
            [clojure.string]
            #?(:clj [clojure.java.io :as io])
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
        (pad (* mu/sizex mu/sizey) {:type :wall}))))

#?(:clj
   (defn load-map
     [data [_ file]]
     (if-let [mf (try
                   (slurp (io/resource file))
                   (catch Exception e (println "Exception thrown " (.getMessage e))))]
       (-> data
           (assoc-in [:boards (dec (:level data))] (load-map-from-string mf))
           (assoc-in [:player :position] [10 10]))
       data)))

#?(:clj
   (defn load-save-game
     [filename]
     (try
       (read-string (slurp  (str "tmp/" filename)))
       (catch Exception e (println "Exception thrown " (.getMessage e))))))

#?(:clj
   (defn valid-save-game?
     [file-name]
     (and
      (string? file-name)
      (s/valid? :game/game (load-save-game file-name)))))

#?(:cljs
   (defn load-game
     [state [_ input]]
     (cond
       (s/valid? :game/game input) input
       :else state)))

#?(:clj
   (defn load-game
     [state [_ input]]
     (cond
       (s/valid? :game/game input) input
       (valid-save-game? input) (load-save-game input)
       :else state)))

#?(:clj
   (defn save-game
     [state [_ filename]]
     (spit (str "tmp/" filename) state)
     state))

#?(:clj
   (defn load-rest-interface
     [filename]
     (when (valid-save-game? filename)
       (load-save-game filename))))
