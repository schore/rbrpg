(ns lum.game.event-test
  (:require [clojure.test :as t :refer [deftest is testing]]
            [lum.game-logic-dsl :as dsl]))

(t/use-fixtures :each dsl/create-game)

(deftest new-map-event
  (dsl/player-is-on :stair-down)
  (dsl/activate 20 20)
  (is (= :enter-unknown-level (get-in (dsl/get-event) [0 0]))))

(deftest correct-level-requested
  (doseq [i [1 3 5]]
    (testing (str "correct level " i)
      (dsl/player-is-on-level i)
      (dsl/player-is-on :stair-down)
      (dsl/activate 20 20)
      (is (= (inc i) (get-in (dsl/get-event) [0 1]))))))
