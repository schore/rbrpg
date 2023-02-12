(ns lum.load-save-test
  (:require
   [clojure.test :as t :refer [deftest is]]
   ;;[etaoin.api :as e]
   ;;[etaoin.keys]
   [lum.common :as c]))

(t/use-fixtures :once c/fixture-start-server c/fixture-driver c/open-website)
(t/use-fixtures :each c/fixture-prepare-directory c/refresh)

(deftest ^:integration load-game
  (c/load-game "got-two-batblood.edn")
  (is (= 2 (get (c/get-items) "batblood"))))

(deftest ^:integration show-docs
  (c/click-menu-item "Help")
  (is (= "Round Based RPG" (c/get-heading))))

(deftest ^:integration initialize
  (is (= :stair-down (c/get-tile))))

(deftest ^:integration new-board
  (c/on-test-map)
  (is (= :stair-down (c/get-tile))))

(deftest ^:integration new-map
  (let [pos (c/get-player-position)]
    (c/new-map)
    (is (= :stair-down (c/get-tile)))
    (is (not= pos (c/get-player-position)))))
