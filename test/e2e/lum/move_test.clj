(ns lum.move-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [etaoin.api :as e]
   [lum.common :as c]))

(t/use-fixtures :once c/fixture-start-server c/fixture-driver c/open-website)

(t/use-fixtures
  :each c/refresh c/game-screen)

(deftest ^:integration game-load
    ;;(navigate-to-game *driver*)
  (is (= [10 10] (c/get-player-position))))

(deftest ^:integration navigate-left
  (c/move :left)
  (is (= [9 10] (c/get-player-position))))

(deftest ^:integration navigate-right
  (c/move :right)
  (is (= [11 10] (c/get-player-position))))

(deftest ^:integration navigate-up
  (c/move :down)
  (is (= [10 11] (c/get-player-position))))

(deftest ^:integration navigate-down
  (c/move :up)
  (is (= [10 9] (c/get-player-position))))
