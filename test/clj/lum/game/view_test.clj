(ns lum.game.view-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game-logic-dsl :as dsl]))

(t/use-fixtures
  :each dsl/create-game)

(deftest can-view-field
  (doseq [[x y] [[10 10]]]
    (testing (str [x y])
      (dsl/test-map-loaded 10 10)
      (is (dsl/field-visible? x y)))))
