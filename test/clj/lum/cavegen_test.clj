(ns lum.cavegen-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game.cavegen :as cg]
   [lum.maputil :as mu]))

(deftest count-neighbours
  (testing "all empty cells"
    (let [m (repeat (* cg/xsize cg/ysize) nil)]
      (is (= 0 (cg/count-neighbours m 1 1)))
      (is (= 5 (cg/count-neighbours m 0 0)))
      (is (= 3 (cg/count-neighbours m 1 0)))
      (is (= 3 (cg/count-neighbours m 0 1)))
      (is (= 5 (cg/count-neighbours m 49 29)))
      (is (= 3 (cg/count-neighbours m 35 0)))))
  (testing "all cell full"
    (let [m (repeat (* cg/xsize cg/ysize) :wall)]
      (is (= 9 (cg/count-neighbours m 0 0)))
      (is (= 9 (cg/count-neighbours m 10 10)))
      (is (= 9 (cg/count-neighbours m 0 90)))
      (is (= 9 (cg/count-neighbours m 0 29)))
      (is (= 9 (cg/count-neighbours m 49 0)))))
  (testing "horizontal walls Pattern .#.#..."
    (let [m (concat (repeat cg/xsize nil)
                    (repeat cg/xsize :wall)
                    (repeat cg/xsize nil)
                    (repeat cg/xsize :wall)
                    (repeat cg/xsize nil)
                    (repeat cg/xsize nil)
                    (repeat cg/xsize nil))]
      (is (= 7 (cg/count-neighbours m 0 0)))
      (is (= 6 (cg/count-neighbours m 1 0)))
      (is (= 3 (cg/count-neighbours m 1 1)))
      (is (= 6 (cg/count-neighbours m 1 2)))
      (is (= 0 (cg/count-neighbours m 1 5)))))
  (testing "vertical wall patterns"
    (let [m (concat (repeat cg/ysize nil)
                    (repeat cg/ysize :wall)
                    (repeat cg/ysize nil)
                    (repeat cg/ysize :wall)
                    (repeat (* cg/ysize (- cg/xsize 4)) nil))
          m (flatten (apply map list (partition cg/ysize m)))]
      (is (= 7 (cg/count-neighbours m 0 0)))
      (is (= 6 (cg/count-neighbours m 0 1)))
      (is (= 3 (cg/count-neighbours m 1 1)))
      (is (= 6 (cg/count-neighbours m 2 1)))
      (is (= 3 (cg/count-neighbours m 3 1)))
      (is (= 3 (cg/count-neighbours m 4 1)))
      (is (= 0 (cg/count-neighbours m 5 1))))))


(deftest map-to-count
  (testing "all empty cells"
    (let [m (cg/map-to-count (repeat (* cg/xsize cg/ysize) nil))]
      (is (= 0 (mu/get-tile m 1 1)))
      (is (= 5 (mu/get-tile m 0 0)))
      (is (= 3 (mu/get-tile m 1 0)))
      (is (= 3 (mu/get-tile m 0 1)))
      (is (= 5 (mu/get-tile m 49 29)))))
  (testing "all cell full"
    (let [m (cg/map-to-count (repeat (* cg/xsize cg/ysize) :wall))]
      (is (= 9 (mu/get-tile m 0 0)))
      (is (= 9 (mu/get-tile m 10 10)))
      (is (= 9 (mu/get-tile m 0 29)))
      (is (= 9 (mu/get-tile m 49 0)))))
  (testing "horizontal walls Pattern .#.#..."
    (let [m (cg/map-to-count (concat (repeat cg/xsize nil)
                                     (repeat cg/xsize :wall)
                                     (repeat cg/xsize nil)
                                     (repeat cg/xsize :wall)
                                     (repeat cg/xsize nil)
                                     (repeat cg/xsize nil)
                                     (repeat cg/xsize nil)))]
      (is (= 7 (mu/get-tile m 0 0)))
      (is (= 6 (mu/get-tile m 1 0)))
      (is (= 3 (mu/get-tile m 1 1)))
      (is (= 6 (mu/get-tile m 1 2)))
      (is (= 0 (mu/get-tile m 1 5)))))
  (testing "vertical wall patterns"
    (let [m (concat (repeat cg/ysize nil)
                    (repeat cg/ysize :wall)
                    (repeat cg/ysize nil)
                    (repeat cg/ysize :wall)
                    (repeat (* cg/ysize (- cg/xsize 4)) nil))
          m (cg/map-to-count (flatten (apply map list (partition cg/ysize m))))]
      (is (= 7 (mu/get-tile m 0 0)))
      (is (= 6 (mu/get-tile m 0 1)))
      (is (= 3 (mu/get-tile m 1 1)))
      (is (= 6 (mu/get-tile m 2 1)))
      (is (= 3 (mu/get-tile m 3 1)))
      (is (= 3 (mu/get-tile m 4 1)))
      (is (= 0 (mu/get-tile m 5 1))))))
