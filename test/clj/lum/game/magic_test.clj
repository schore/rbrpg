(ns lum.game.magic-test
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

(deftest magic-can-end-fights-with-damage
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands" 1 1 3 1 1 1 1)
  (is (not (dsl/in-fight?))))

(deftest magic-makes-damage-according-to-rolls
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands" 1 1 1 1)
  (is (= 2 (dsl/get-enemy-hp))))

(deftest magic-burns-hp
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands")
  (is (= 0 (dsl/get-mp))))

(deftest attacked-after-magic
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands" 1 1 1 15 1)
  (is (< (dsl/get-hp) 10)))

(deftest can-not-cast-unknown-spells
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Magic missle")
  (is (= 5 (dsl/get-enemy-hp))))

(deftest can-not-cast-with-insufficen-mana
  (dsl/player-has-mp 0)
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands")
  (is (dsl/in-fight?)))

(deftest learning-new-spells-from-items
  (dsl/player-has-items {"force scroll" 1})
  (dsl/use-item "force scroll")
  (is (contains? (dsl/get-spells) "Force")))

(deftest restore-mp
  (dsl/player-has-mp 0)
  (dsl/player-has-items {"small mana potion" 1})
  (dsl/use-item "small mana potion")
  (is (= 3 (dsl/get-mp))))

(deftest healing-spell-during-fight
  (dsl/in-a-fight)
  (dsl/player-has-hp 1)
  (dsl/player-know-spell "Healing")
  (dsl/cast-spell "Healing" 2)
  (is (> (dsl/get-hp) 1)))

(deftest healing-spell-during-moving
  (dsl/player-has-hp 1)
  (dsl/player-know-spell "Healing")
  (dsl/cast-spell "Healing")
  (is (> (dsl/get-hp) 1)))

(deftest attack-spell-not-possible
  (dsl/game-is-initialized)
  (dsl/cast-spell "Burning Hands")
  (is (= 3 (dsl/get-mp))))

(deftest magic-mapping-spell
  (dsl/player-know-spell "Magic Mapping")
  (dsl/cast-spell "Magic Mapping")
  (is (dsl/board-completly-visible?)))
