(ns lum.game.map-test
  (:require
   [clojure.string]
   [clojure.test :as t :refer [deftest is]]
   [lum.game-logic-dsl :as dsl]
   [lum.game.dataspec]))

(t/use-fixtures
  :each dsl/create-game)

(deftest level-5-is-special-map
  (dsl/player-is-on-special-map)
  (is (= [22 19] (dsl/get-position))))

(deftest trigger-a-message
  (dsl/player-is-on-special-map)
  (dsl/player-steps-on-message-trigger)
  (is (= "A board squeezes" (dsl/get-last-message))))

(deftest only-trigger-a-message-on-trigger
  (dsl/player-is-on-special-map)
  (dsl/move :right)
  (is (not= "A board squeezes" (dsl/get-last-message))))

(deftest trigger-a-fight
  (dsl/player-is-on-special-map)
  (dsl/player-steps-on-fight-trigger)
  (is (dsl/in-fight?)))

(deftest special-item-on-field
  (dsl/player-is-on-special-map)
  (is (contains? (:items (dsl/get-tile 15 15)) "magic mapping scroll")))
