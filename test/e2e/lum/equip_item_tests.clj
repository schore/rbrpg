(ns e2e.lum.equip-item-tests
  (:require
   [clojure.test :as t :refer [deftest is]]
   ;[etaoin.api :as e]
   [etaoin.keys]
   [lum.common :as c :refer [*driver*]]
   ;[clojure.tools.logging :as log]
   ))

(t/use-fixtures :once
  c/fixture-start-server
  c/fixture-driver c/open-website)

(t/use-fixtures :each c/fixture-prepare-directory c/refresh)


(deftest ^:integration selcet-sword
  (c/equip *driver* "right-hand" "sword")
  (is (= (c/get-equipment-slot *driver* "right-hand") "sword")))

(deftest ^:integration unselect-sword
  (c/equip *driver* "right-hand" "sword")
  (c/equip *driver* "right-hand" "none")
  (is (= (c/get-equipment-slot *driver* "right-hand") "none")))

(deftest ^:integration use-equipped-item
  (c/equip *driver* "right-hand" "sword")
  (c/use-item *driver* "sword")
  (is (= (c/get-equipment-slot *driver* "right-hand") "none")))
