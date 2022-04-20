(ns lum.game.load-save-test
  (:require [lum.game-logic-dsl :refer [create-game
                                        game-is-initialized
                                        initalize-game
                                        load-game
                                        save-game
                                        get-items
                                        get-state]]
            [lum.game.cavegen :as cavegen]
            [clojure.test :as t :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))


(defn delete-directory-recursive
  "Recursively delete a directory."
  [^java.io.File file]
  ;; when `file` is a directory, list its entries and call this
  ;; function with each entry. can't `recur` here as it's not a tail
  ;; position, sadly. could cause a stack overflow for many entries?
  ;; thanks to @nikolavojicic for the idea to use `run!` instead of
  ;; `doseq` :)
  (when (.isDirectory file)
    (run! delete-directory-recursive (.listFiles file)))
  ;; delete the file or directory. if it it's a file, it's easily
  ;; deletable. if it's a directory, we already have deleted all its
  ;; contents with the code above (remember?)
  (io/delete-file file))

(defn prepare-directory
  [f]
  (.mkdir (io/file "tmp"))
  (f)
  (delete-directory-recursive (io/file "tmp")))


(t/use-fixtures
  :each prepare-directory create-game)


(defn prepare-save-game
  [filename]
  (spit (str "tmp/" filename)
        (slurp (io/resource (str "savegames/" filename)))))

(deftest load-game-with-state
  (testing "Load a game"
    (let [game-state (loop [game-state nil]
                       (if (s/valid? :game/game game-state)
                         game-state
                         (recur {:board (cavegen/get-dungeon)
                                 :messages '("")
                                 :player {:position [12 12]
                                          :ac 5
                                          :xp 0
                                          :hp [10 10]
                                          :mp [3 3]
                                          :equipment {}
                                          :items {}}})))]
      (game-is-initialized)
      (load-game game-state)
      (is (= game-state (get-state))))))

(deftest load-of-invalide-game-data-prevented
  (game-is-initialized)
  (load-game {})
  (is (s/valid? :game/game (get-state))))


(deftest load-saved-game
  (prepare-save-game "load-test.edn")
  (load-game "load-test.edn")
  (is (s/valid? :game/game (get-state)))
  (is (= 1 (get (get-items) "small healing potion" 0))))

(deftest save-game-and-able-to-reload
  (let [filename "1.edn"
        state (game-is-initialized)]
    (save-game filename)
    (initalize-game)
    (is (= state (load-game filename)))))

(deftest valid-state-after-save-game
  (game-is-initialized)
  (save-game "test.edn")
  (is (s/valid? :game/game (get-state))))
