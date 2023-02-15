(ns lum.game.event-test
  (:require [clojure.test :as t :refer [deftest is]]
            [lum.game-logic-dsl :as dsl]))

(t/use-fixtures :each dsl/create-game)

(deftest new-map-event
  (dsl/player-is-on :stair-down)
  (dsl/activate)
  (is (= [[:enter-unknown-level 2]] (dsl/get-event))))
