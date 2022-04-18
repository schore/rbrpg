(ns lum.game.load-save-test
  (:require [lum.game-logic-dsl :refer [create-game
                                        game-is-initialized
                                        load-game
                                        get-items
                                        get-state]]
            [lum.game.cavegen :as cavegen]
            [clojure.test :as t :refer [deftest testing is]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(t/use-fixtures
  :each create-game)


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
  (load-game "savegames/load-test.edn")
  (is (s/valid? :game/game (get-state)))
  (is (= 1 (get (get-items) "small healing potion" 0))))
