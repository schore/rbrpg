(ns lum.game.load-save-test
  (:require [lum.game-logic-dsl :as dsl]
            [lum.game.cavegen :as cavegen]
            [clojure.test :as t :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

(t/use-fixtures
  :each dsl/prepare-directory dsl/create-game)

(deftest load-game-with-state
  (testing "Load a game"
    (let [game-state (loop [game-state nil]
                       (if (s/valid? :game/game game-state)
                         game-state
                         (recur {:boards [(cavegen/get-dungeon)]
                                 :level 1
                                 :messages '("")
                                 :player {:position [12 12]
                                          :ac 5
                                          :xp 0
                                          :hp [10 10]
                                          :mp [3 3]
                                          :spells #{}
                                          :equipment {}
                                          :items {}}})))]
      (dsl/game-is-initialized)
      (dsl/load-game game-state)
      (is (= game-state (dsl/get-state))))))

(deftest load-of-invalide-game-data-prevented
  (dsl/game-is-initialized)
  (dsl/load-game {})
  (is (s/valid? :game/game (dsl/get-state))))

(deftest load-saved-game
  (dsl/prepare-save-game "load-test.edn")
  (dsl/load-game "load-test.edn")
  (is (s/valid? :game/game (dsl/get-state)))
  (is (= 1 (get (dsl/get-items) "small healing potion" 0))))

(deftest save-game-and-able-to-reload
  (let [filename "1.edn"
        state (dsl/game-is-initialized)]
    (dsl/save-game filename)
    (dsl/initalize-game)
    (is (= state (dsl/load-game filename)))))

(deftest valid-state-after-save-game
  (dsl/game-is-initialized)
  (dsl/save-game "test.edn")
  (is (s/valid? :game/game (dsl/get-state))))
