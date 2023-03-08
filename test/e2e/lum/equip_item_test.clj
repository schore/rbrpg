(ns lum.equip-item-test
  (:require
   [clojure.test :as t :refer [deftest is]]
   ;[etaoin.api :as e]
   [etaoin.keys]
   [lum.common :as c]
   ;[clojure.tools.logging :as log]
   ))

(t/use-fixtures :once
  c/fixture-start-server
  c/fixture-driver)

(t/use-fixtures :each c/fixture-prepare-directory c/open-website)

(deftest ^:integration selcet-sword
  (c/load-game "got-two-batblood.edn")
  (c/equip "right-hand" "sword")
  (is (= (c/get-equipment-slot "right-hand") "sword")))

(deftest ^:integration unselect-sword
  (c/load-game "got-two-batblood.edn")
  (c/equip "right-hand" "sword")
  (c/equip "right-hand" "none")
  (is (= (c/get-equipment-slot "right-hand") "none")))

(deftest ^:integration use-equipped-item
  (c/load-game "on-stairs-to-special-map.edn")
  (c/equip "right-hand" "small healing potion")
  (c/use-item "small healing potion")
  (is (= (c/get-equipment-slot "right-hand") "none")))
