(ns lum.move-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [etaoin.api :as e]
   [lum.common :as c :refer [*driver*]]))

(t/use-fixtures :once c/fixture-driver c/open-website)

(t/use-fixtures
  :each c/refresh c/game-screen)

(deftest game-load
    ;;(navigate-to-game *driver*)
  (is (= [10 10] (c/get-player-position *driver*))))

(deftest navigate-left
  (c/move *driver* :left)
  (is (= [9 10] (c/get-player-position *driver*))))

(deftest navigate-right
  (c/move *driver* :right)
  (is (= [11 10] (c/get-player-position *driver*))))

(deftest navigate-up
  (c/move *driver* :down)
  (is (= [10 11] (c/get-player-position *driver*))))

(deftest navigate-down
  (c/move *driver* :up)
  (is (= [10 9] (c/get-player-position *driver*))))
