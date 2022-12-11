(ns lum.game.player-attack-test
  (:require  [clojure.test :as t :refer [deftest testing is]]
             [lum.game.gamelogic :as g]
             [lum.game.utilities :as u]
             [lum.game.fight :as f]))

(defn attack
  [f data & rolls]
  (let [r (atom rolls)]
    (with-redefs [u/roll (fn [_]
                           (let [f (first @r)]
                             (swap! r rest)
                             f))]
      (f data))))

(def gamestate
  {:fight {:enemy {:hp [1 1]
                   :ac 10}}})

(deftest attack-role
  (defn get-damage [& rolls]
    (get-in (apply attack f/player-attacks gamestate rolls) [:fight :enemy :hp 0]))
  (testing "A 20 always hits and gives double roles"
    (is (= 0 (get-damage 20 1 2))))
  (testing "A 1 never hits"
    (is (= 1 (get-damage 1 1 1))))
  (testing "Miss because of AC"
    (is (= 1 (get-damage 10 1 1))))
  (testing "Regular hit"
    (is (= 0 (get-damage 12 1 2)))))

(deftest check-enemy-hit
  (defn hit? [ac roll]
    (= 0
       (get-in (attack f/player-attacks
                       {:fight {:enemy {:name "Bat" :ac ac :hp [1 1]}}}
                       roll 1 1 1 1 1 1 1 1 1 1 1 1)
               [:fight :enemy :hp 0])))
  (is (hit? 50 20))
  (is (hit? 20 20))
  (is (hit? 5 6))
  (is (not (hit? 10 10)))
  (is (not (hit? -1 1)))
  (is (not (hit? 10 1))))

(deftest check-player-hit
  (defn hit? [ac roll]
    (= 0
       (get-in (attack f/enemy-attacks
                       {:fight {:enemy {:name "Bat" :ac 30
                                        :hp [1 1]}}
                        :player {:ac ac
                                 :hp [1 10]}}
                       roll 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1)
               [:player :hp 0])))
  (is (hit? 5 6))
  (is (hit? 50 20))
  (is (not (hit? -10 1)))
  (is (not (hit? 2 2))))

(defn possible-enemis-on-level
  [level]
  (into [] (f/possible-enemies {:level (dec level)})))

(deftest daemon-not-before-level-5
  (is (not (some #{"Deamon"} (possible-enemis-on-level 1))))
  (is  (some #{"Deamon"} (possible-enemis-on-level 5))))
