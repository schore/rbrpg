(ns lum.game.interaction-test
  (:require  [clojure.test :as t :refer [deftest is]]
             [lum.game-logic-dsl :as dsl]))

(t/use-fixtures
  :each dsl/create-game)

(deftest start-interaction
  (dsl/player-with-npc "Foo")
  (dsl/activate)
  (is (dsl/in-interaction?)))

(deftest start-no-interaction-without-npc
  (dsl/player-is-on :ground)
  (dsl/activate)
  (is (not (dsl/in-interaction?))))
