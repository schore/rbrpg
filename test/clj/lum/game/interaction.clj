(ns lum.game.interaction
  (:require  [clojure.test :as t :refer [deftest is]]
             [lum.game-logic-dsl :as dsl]))

(t/use-fixtures
  :each dsl/create-game)

(deftest start-interaction
  (dsl/player-with-npc "Foo")
  (dsl/activate)
  (is (dsl/in-interaction?)))
