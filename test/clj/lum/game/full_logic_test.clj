(ns lum.game.full-logic-test
  (:require
   [clojure.test :as t :refer [deftest is]]
   [lum.game-logic-dsl :as dsl]))

(t/use-fixtures
  :each dsl/create-game-with-chan)

(deftest level-2-can-be-entered-entered
  (dsl/player-is-on :stair-down)
  (dsl/activate)
  (is (= 2 (dsl/get-level))))

(deftest level-2-not-entered-on-ground
  (dsl/player-is-on :ground)
  (dsl/activate)
  (is (= 1 (dsl/get-level))))
