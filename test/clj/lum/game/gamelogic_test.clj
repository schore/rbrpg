(ns lum.game.gamelogic-test
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

(deftest initalize-tests
  (is (s/valid? :game/game (dsl/game-is-initialized))))

(deftest load-map-test
  (testing "load a map from file"
    (dsl/test-map-loaded)
    (let [m (dsl/get-board)]
      ;; check if map is valid
      (is (s/valid? :game/board m))
      ;;(log/info (s/explain :game/board m))
      ;;only some examples
      (is (= :ground (:type (first m))))
      (is (= :wall (:type (mu/get-tile m 3 5))))
      (is (= :ground (:type (mu/get-tile m 0 2))))
      (is (= :stair-down (:type (mu/get-tile m 21 19))))
      (is (= :stair-up (:type (mu/get-tile m 22 19)))))))

(deftest create-new-board
  (dsl/game-is-initialized)
  (let [old-board (dsl/get-board)]
    (dsl/new-board)
    (is (s/valid? :game/game (dsl/get-state)))
    (is (not= old-board
              (dsl/get-board)))))

(deftest player-can-increase-level
  (dsl/player-has-xp 99)
  (dsl/get-one-xp)
  (is (= 16 (dsl/get-max-hp))))

(deftest player-increase-mp-by-leveling-up
  (dsl/player-has-xp 99)
  (dsl/get-one-xp)
  (is (= 6 (dsl/get-max-mp))))

(deftest player-hp-does-not-increase-on-regular-xp-updates
  (dsl/player-has-xp 80)
  (dsl/get-one-xp)
  (is (= 10 (dsl/get-max-hp))))

