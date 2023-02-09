(ns lum.game.fight-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string]
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game-logic-dsl :as dsl]
   [lum.game.dataspec]
   [lum.maputil :as mu]
   [lum.game.game-database :as db]))

(t/use-fixtures
  :each dsl/create-game)

(deftest get-in-a-fight
  (dsl/game-is-initialized)
  (dsl/move-and-get-attacked)
  (is (dsl/in-fight?)))

(deftest kill-it
  (dsl/in-a-fight)
  ;; You kill it with the first strike
  (dsl/attack 20 2 2 1 1)
  (is (= 10 (dsl/get-hp)))
  (is (= 1 (dsl/get-xp)))
  (is (not (dsl/in-fight?))))

(deftest get-killed-by-enemy
  (dsl/in-a-fight)
  (dsl/attack 1 20 2 2)
  (dsl/attack 1 20 2 2)
  (dsl/attack 1 20 2 2)
  (is (= 0 (dsl/get-hp)))
  (is (dsl/game-over?)))

(deftest hit-by-enemy
  (dsl/in-a-fight)
  (dsl/attack 15 1 15 2)
  (is (= 8 (dsl/get-hp)))
  (is (= 1 (dsl/get-enemy-hp))))

(deftest enemy-always-hit-with-20
  (dsl/in-a-fight)
  (dsl/attack 1 20 2 2)
  (is (> 10 (dsl/get-hp))))

(deftest enemy-ac<roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 1 15 2 2)
  (is (> 10 (dsl/get-hp))))

(deftest enemy-ac>roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 1 2 2)
  (is (= 10 (dsl/get-hp))))

(deftest enemy-ac=roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 1 5 2)
  (is (= 10 (dsl/get-hp))))

(deftest player-always-hit-with-20
  (dsl/in-a-fight)
  (dsl/attack 20 0 1 1)
  (is (> 2 (dsl/get-enemy-hp))))

(deftest player-ac<roll-hit
  (dsl/in-a-fight)
  (dsl/attack 15 1 0)
  (is (> 2 (dsl/get-enemy-hp))))

(deftest player-ac>roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 9 2 2)
  (is (= 2 (dsl/get-enemy-hp))))

(deftest player-ac=roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 10 5 2)
  (is (= 2 (dsl/get-enemy-hp))))

(deftest armor-protects-from-damage
  (dsl/player-is-equipped :body "leather armor")
  (dsl/move-and-get-attacked "Bat")
  (dsl/attack 1 10 1)
  (is (= 10 (dsl/get-hp))))

(deftest armor-and-shield-protects
  (dsl/player-is-equipped :body "leather armor")
  (dsl/player-is-equipped  :left-hand "wooden shield")
  (dsl/move-and-get-attacked "Bat")
  (dsl/attack 1 12 1)
  (is (= 10 (dsl/get-hp))))

(deftest enemy-makes-correct-damage
  (doseq [[enemy rolls hp] [["Rat" [2] 8]
                            ["Bat" [3] 7]]]
    (testing (str enemy " " rolls " " hp)
      (dsl/initalize-game)
      (dsl/move-and-get-attacked enemy)
      (apply dsl/attack 1 15 rolls)
      (is (= hp (dsl/get-hp))))))

(deftest get-item-after-fight
  (dsl/in-a-fight)
  (dsl/killed-the-enemy)
  (is (= 1 (get (dsl/get-items) "batblood")))
  (is (= 1 (get (dsl/get-items) "batwing"))))

(deftest in-fight-with-a-rat
  (dsl/game-is-initialized)
  (dsl/move-and-get-attacked "Rat")
  (is (= "Rat" (dsl/get-enemy-name)))
  (is (clojure.string/includes? (first (dsl/get-messages)) "Rat")))

(deftest weapon-makes-damage
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/move-and-get-attacked "Bat")
  (dsl/attack 19 5 1 1 1)
  (is (not (dsl/in-fight?))))

(deftest enter-fight-after-activation
  (dsl/game-is-initialized)
  (dsl/activate 1 1 1 1)
  (is (dsl/in-fight?)))

(deftest in-a-fight-fleeing-successfull
  (dsl/in-a-fight "Bandit")
  (dsl/flea 20)
  (is (not (dsl/in-fight?))))

(deftest fleeing-unsuscessfull
  (dsl/in-a-fight "Bandit")
  (dsl/flea 1 1)
  (is (dsl/in-fight?)))

(deftest enemy-attacks-when-fleeing-fails
  (dsl/in-a-fight "Bandit")
  (dsl/flea 1 20 1 1 1 1)
  (is (< (dsl/get-hp) 10)))

(deftest bandit-gives-aromor-if-lucky
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/in-a-fight "Bandit")
  (dsl/attack 20 6 6 15 1 1 1)
  (is (contains? (dsl/get-items) "leather armor")))

(deftest bandit-gives-no-aromor-if-unlucky
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/in-a-fight "Bandit")
  (dsl/attack 20 6 6 1 1 1 1)
  (is (not (contains? (dsl/get-items) "leather armor"))))

(deftest message-about-looted-items
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/in-a-fight "Bandit")
  (dsl/attack 20 6 6 15 1 1 1)
  (is (clojure.string/includes? (first (dsl/get-messages)) "looted")))
