(ns lum.maputil-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [lum.maputil :as m]))


(deftest position-to-number
  (is (= 0 (m/position-to-n 0 0)))
  (is (= (* 80 m/sizex) (m/position-to-n 0 80)))
  (is (= 1 (m/position-to-n 1 0) )))

(deftest number-to-positon
  (is (= [0 0] (m/n-to-position 0) ))
  (is (= [5 80] (m/n-to-position (+ 5 (* 80 m/sizex))) )))

(deftest number-to-position-match
  (doseq [i (range 0 (dec (* 80 80)))]
     (is (= i (apply m/position-to-n (m/n-to-position i))))))



(deftest get-tile
  (testing "Check tiles are as expected"
    (let [cg (range 1000)]
      (is (= 0 (m/get-tile cg 0 0)))
      (is (= 10 (m/get-tile cg 10 0)))
      (is (= 50 (m/get-tile cg 0 1)))
      (is (= 51 (m/get-tile cg 1 1))))))

;; (deftest to-map
;;   (testing "Check to map works as expected"
;;     (let [m (into {} (m/to-map (range (* m/sizex m/sizey))))]
;;       (is (= 0 (get m [0 0])))
;;       (is (= 50 (get m [0 1])))
;;       (is (= 10 (get m [10 0])))
;;       (is (= 510 (get m [10 10]))))))

(deftest in-map?
  (testing "Values inside map"
    (is (m/inmap? 0 0))
    (is (m/inmap? 1 1))
    (is (m/inmap? 49 0))
    (is (m/inmap? 0 29))
    (is (m/inmap? 49 29)))
  (testing "negative values"
    (is (not (m/inmap? -1 0)))
    (is (not (m/inmap? 0 -1)))
    (is (not (m/inmap? -10 -10))))
  (testing "out of bound values"
    (is (not (m/inmap? 50 0)))
    (is (not (m/inmap? 0 30)))
    (is (not (m/inmap? -5 90)))))
