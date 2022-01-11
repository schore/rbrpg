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
  {})

(deftest attack-role
  (testing "A 20 always hits and gives double roles"
    (is (> -1 (get-in (attack gamestate 20 1 2) [1 0 :n]))))
  (testing "A 1 never hits"
    (is (= 0 (get-in (attack gamestate 1) [1 0 :n])))))
