(ns lum.game.view-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game-logic-dsl :as dsl]))

(t/use-fixtures
  :each dsl/create-game)

(deftest can-view-field
  (doseq [[x y] [[20 20]
                 [20 25]
                 [23 23]
                 [23 24]
                 [25 20]]]
    (testing (str [x y])
      (dsl/test-map-loaded 20 20)
      (is (dsl/field-visible? x y)))))

(deftest can-not-view-field
  (doseq [[x y] [[1 1]
                 [20 9]
                 [28 28]]]
    (testing (str [x y])
      (dsl/test-map-loaded 20 20)
      (is (not (dsl/field-visible? x y))))))

(deftest view-blocked
  (doseq [[x y] [[1 3]
                 [17 20]
                 [18 20]
                 [18 21]
                 [18 19]
                 [17 19]]]
    (testing (str [x y])
      (dsl/test-map-loaded 20 19)
      (is (not (dsl/field-visible? x y))))))

(deftest stairs-don't-block-view
  (dsl/test-map-loaded 20 19)
  (is (dsl/field-visible? 24 19)))

(deftest can-view-first-wall
  (doseq [[x y] [[19 20]
                 [19 21]]]
    (testing (str [x y])
      (dsl/test-map-loaded 20 20)
      (is  (dsl/field-visible? x y)))))
