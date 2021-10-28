(ns lum.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [pjstadig.humane-test-output]
            [lum.picture-game :as pg]
            ))

(deftest test-home
  (is (= true true)))

(deftest position-to-number
  (is (= 0 (pg/position-to-n 0 0)))
  (is (= (* 80 pg/size-x) (pg/position-to-n 0 80)))
  (is (= 1 (pg/position-to-n 1 0) )))

(deftest number-to-positon
  (is (= [0 0] (pg/n-to-position 0) ))
  (is (= [5 80] (pg/n-to-position (+ 5 (* 80 pg/size-x))) )))

(deftest number-to-position-match
  (doseq [i (range 0 (dec (* 80 80)))]
     (is (= i (apply pg/position-to-n (pg/n-to-position i))))))
