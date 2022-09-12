(ns lum.game.view-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game-logic-dsl :as dsl]))

(t/use-fixtures
  :each dsl/create-game)

(deftest can-view-field
  (doseq [[x y] [[10 10]
                 [10 15]
                 [13 13]
                 [13 14]
                 [15 10]]]
    (testing (str [x y])
      (dsl/test-map-loaded 10 10)
      (is (dsl/field-visible? x y)))))

(deftest can-not-view-field
  (doseq [[x y] [[1 1]
                 [5 1]
                 [10 1]
                 [14 14]
                 [16 10]
                 [10 16]
                 [10 4]]]
    (testing (str [x y])
      (dsl/test-map-loaded 10 10)
      (is (not (dsl/field-visible? x y))))))
