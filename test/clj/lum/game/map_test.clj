(ns lum.game.map-test
  (:require
   [clojure.string]
   [clojure.test :as t :refer [deftest is]]
   [lum.game-logic-dsl :as dsl]
   [lum.game.dataspec]))

(t/use-fixtures
  :each dsl/create-game)

(deftest level-5-is-special-map
  (dsl/player-is-on-level 5)
  (is (= [22 19] (dsl/get-position))))
