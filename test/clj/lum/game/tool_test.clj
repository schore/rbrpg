(ns lum.game.tool-test
  (:require [clojure.edn :as edn]
            [lum.game.dataspec]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]))

(defn add-visible-to-board
  [board]
  (vec
   (map #(merge % {:visible? false}) board)))

(defn add-visible-to-boards
  [boards]
  (vec
   (map add-visible-to-board boards)))

(defn move-recepies-to-player
  [game]
  (-> game
      (assoc-in [:player :recepies] (:recepies game))
      (dissoc :recepies)))

(defn reorganise-boards
  [game]
  (-> game
      (dissoc :boards)
      (dissoc :level)
      (update-in [:player] #(dissoc % :position))
      (assoc :board {:dungeons (:boards game)
                     :player-position [(:level game)
                                       (get-in game [:player :position 0])
                                       (get-in game [:player :position 1])]})))

(defn update-save-game
  ([file update-function]
   (let [state (edn/read-string (slurp file))]
     (spit file (update-function state)))))

(defn convert
  ([]
   (convert #(update % :boards add-visible-to-boards)))
  ([update-function]
   (let [directory (io/file "resources/savegames/")
         files (file-seq directory)]
     (doseq [file files]
       (when (.isFile file)
         (println file)
         (update-save-game (.getPath file) update-function))))))

(defn add-empty-recipies
  []
  (convert #(assoc % :recepies [])))

(s/explain :game/game (edn/read-string  (slurp "resources/savegames/got-two-batblood.edn")))
