(ns lum.move-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [etaoin.api :as e]
   [lum.common :as c]))

(t/use-fixtures :once
  c/fixture-prepare-directory
  c/fixture-start-server
  c/fixture-driver
  c/open-website)

(t/use-fixtures
  :each c/refresh)

(deftest ^:integration game-load
  (c/on-test-map)
  (is (= [10 10] (c/get-player-position))))

(deftest ^:integration navigate-left
  (c/on-test-map)
  (c/move :left)
  (is (= [9 10] (c/get-player-position))))

(deftest ^:integration navigate-right
  (c/on-test-map)
  (c/move :right)
  (is (= [11 10] (c/get-player-position))))

(deftest ^:integration navigate-up
  (c/on-test-map)
  (c/move :down)
  (is (= [10 11] (c/get-player-position))))

(deftest ^:integration navigate-down
  (c/on-test-map)
  (c/move :up)
  (is (= [10 9] (c/get-player-position))))

(deftest ^:integration enter-next-level
  (c/load-game "on-stairs.edn")
  (let [prev-pos (c/get-player-position)]
    (c/activate)
    (is (not= prev-pos (c/get-player-position)))))
