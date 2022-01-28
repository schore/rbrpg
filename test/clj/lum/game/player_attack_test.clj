(ns lum.game.player-attack-test
  (:require  [clojure.test :as t :refer [deftest testing is]]
             [lum.game.gamelogic :as g]))


(defn attack
  [data & rolls]
  (let [r (atom (map dec rolls))]
    (with-redefs [rand-int (fn [_]
                             (let [f (first @r)]
                               (swap! r rest)
                               f))]
      (g/player-attacks data))))

(def gamestate
  {:fight { :enemy {:ac 10} }})



(deftest attack-role
  (defn get-damage [& rolls]
    (get-in (apply attack gamestate rolls) [1 0 :n]))
  (testing "A 20 always hits and gives double roles"
    (is (> -1 (get-damage 20 1 2))))
  (testing "A 1 never hits"
    (is (= 0 (get-damage 1))))
  (testing "Miss because of AC"
    (is (= 0 (get-damage 10))))
  (testing "Regular hit"
    (is (= -1 (get-damage 11 1 2)))))


(deftest check-hit
  (defn hit? [ac roll]
    (> 0
       (get-in (attack {:fight {:enemy {:ac ac}}}
                       roll 1 1 1 1 1 1 1 1 1 1 1 1)
               [1 0 :n])))
  (is (hit? 50 20))
  (is (hit? 20 20))
  (is (hit? 5 6))
  (is (not (hit? 10 10)))
  (is (not (hit? -1 1)))
  (is (not (hit? 10 1))))
