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

(defn move-with-retry
  [direction]
  (loop [i 0]
    (c/refresh #())
    (c/load-game "test-map.edn")
    (c/move direction)
    (when (and (< i 20)
               (c/fight-screen?))
      (recur (inc i)))))

(deftest ^:integration game-load
  (c/load-game "test-map.edn")
  (is (= [10 10] (c/get-player-position))))

(deftest ^:integration navigate-left
  (c/load-game "test-map.edn")
  (move-with-retry :left)
  (is (= [9 10] (c/get-player-position))))

(deftest ^:integration navigate-right
  (move-with-retry :right)
  (is (= [11 10] (c/get-player-position))))

(deftest ^:integration navigate-up
  (move-with-retry :up)
  (is (= [10 9] (c/get-player-position))))

(deftest ^:integration navigate-down
  (move-with-retry :down)
  (is (= [10 11] (c/get-player-position))))

(deftest ^:integration enter-next-level
  (c/load-game "on-stairs.edn")
  (let [prev-pos (c/get-player-position)]
    (c/activate)
    (is (not= prev-pos (c/get-player-position)))))
