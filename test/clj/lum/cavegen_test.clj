(ns lum.cavegen-test
  (:require  [clojure.test :as t :refer [deftest is testing]]
             [lum.routes.game.cavegen :as cg]))


(deftest get-tile
  (testing "Check tiles are as expected"
    (let [m (range 1000)]
      (is (= 0 (cg/get-tile m 0 0)))
      (is (= 10 (cg/get-tile m 10 0)))
      (is (= 50 (cg/get-tile m 0 1)))
      (is (= 51 (cg/get-tile m 1 1))))))

(deftest to-map
  (testing "Check to map works as expected"
    (let [m (into {} (cg/to-map (range (* cg/xsize cg/ysize))))]
      (is (= 0 (get m [0 0])))
      (is (= 50 (get m [0 1])))
      (is (= 10 (get m [10 0])))
      (is (= 510 (get m [10 10]))))))

(deftest in-map?
  (testing "Values inside map"
    (is (cg/inmap? 0 0))
    (is (cg/inmap? 1 1))
    (is (cg/inmap? 49 0))
    (is (cg/inmap? 0 29))
    (is (cg/inmap? 49 29)))
  (testing "negative values"
    (is (not (cg/inmap? -1 0)))
    (is (not (cg/inmap? 0 -1)))
    (is (not (cg/inmap? -10 -10))))
  (testing "out of bound values"
    (is (not (cg/inmap? 50 0)))
    (is (not (cg/inmap? 0 30)))
    (is (not (cg/inmap? -5 90)))))


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
      (is (= 0 (cg/get-tile m 1 1)))
      (is (= 5 (cg/get-tile m 0 0)))
      (is (= 3 (cg/get-tile m 1 0)))
      (is (= 3 (cg/get-tile m 0 1)))
      (is (= 5 (cg/get-tile m 49 29)))))
  (testing "all cell full"
    (let [m (cg/map-to-count (repeat (* cg/xsize cg/ysize) :wall))]
      (is (= 9 (cg/get-tile m 0 0)))
      (is (= 9 (cg/get-tile m 10 10)))
      (is (= 9 (cg/get-tile m 0 29)))
      (is (= 9 (cg/get-tile m 49 0)))))
  (testing "horizontal walls Pattern .#.#..."
    (let [m (cg/map-to-count (concat (repeat cg/xsize nil)
                                     (repeat cg/xsize :wall)
                                     (repeat cg/xsize nil)
                                     (repeat cg/xsize :wall)
                                     (repeat cg/xsize nil)
                                     (repeat cg/xsize nil)
                                     (repeat cg/xsize nil)))]
      (is (= 7 (cg/get-tile m 0 0)))
      (is (= 6 (cg/get-tile m 1 0)))
      (is (= 3 (cg/get-tile m 1 1)))
      (is (= 6 (cg/get-tile m 1 2)))
      (is (= 0 (cg/get-tile m 1 5)))))
  (testing "vertical wall patterns"
    (let [m (concat (repeat cg/ysize nil)
                    (repeat cg/ysize :wall)
                    (repeat cg/ysize nil)
                    (repeat cg/ysize :wall)
                    (repeat (* cg/ysize (- cg/xsize 4)) nil))
          m (cg/map-to-count (flatten (apply map list (partition cg/ysize m))))]
      (is (= 7 (cg/get-tile m 0 0)))
      (is (= 6 (cg/get-tile m 0 1)))
      (is (= 3 (cg/get-tile m 1 1)))
      (is (= 6 (cg/get-tile m 2 1)))
      (is (= 3 (cg/get-tile m 3 1)))
      (is (= 3 (cg/get-tile m 4 1)))
      (is (= 0 (cg/get-tile m 5 1))))))
