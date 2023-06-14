(ns lum.move-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [etaoin.api :as e]
   [lum.common :as c]))

(t/use-fixtures :once
  c/fixture-prepare-directory
  c/fixture-start-server
  c/fixture-driver)

(t/use-fixtures
  :each c/open-website)

(defn move-with-retry
  [direction]
  (loop [i 0]
    (c/open-website #())
    (c/load-game "test-map.edn")
    (c/move direction)
    (when (and (< i 20)
               (c/fight-screen?))
      (recur (inc i)))))

(deftest ^:integration game-load
  (c/load-game "test-map.edn")
  (is (= [10 10] (c/get-player-position))))

(deftest ^:integration navigate-left
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

(deftest ^:integration navigate-down-left
  (move-with-retry :down-left)
  (is (= [9 11] (c/get-player-position))))

(deftest ^:integration navigate-down-right
  (move-with-retry :down-right)
  (is (= [11 11] (c/get-player-position))))

(deftest ^:integration navigate-up-left
  (move-with-retry :up-left)
  (is (= [9 9] (c/get-player-position))))

(deftest ^:integration navigate-up-right
  (move-with-retry :up-right)
  (is (= [11 9] (c/get-player-position))))

(deftest ^:integration enter-next-level
  (c/load-game "on-stairs.edn")
  (let [prev-pos (c/get-player-position)]
    (c/activate)
    (is (not= prev-pos (c/get-player-position)))))

(deftest ^:integration enter-special-map

  (c/load-game "on-stairs-to-special-map.edn")
  (c/activate)
  (is (= [22 19] (c/get-player-position))))
