(ns lum.load-save-test
  (:require
   [clojure.test :as t :refer [deftest is]]
   ;;[etaoin.api :as e]
   ;;[etaoin.keys]
   [lum.common :as c]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]))


(t/use-fixtures :once c/fixture-start-server c/fixture-driver c/open-website)
(t/use-fixtures :each c/fixture-prepare-directory c/refresh c/game-screen)

(deftest ^:integration load-game
  (c/load-game "got-two-batblood.edn")
  (is (= 2 (get (c/get-items ) "batblood"))))
