(ns lum.game.gamelogic-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [lum.game-logic-dsl :refer [activate
                               attack
                               combine
                               create-game
                               game-is-initialized
                               get-equipped-items
                               load-game
                               game-over?
                               get-board
                               get-enemy-hp
                               get-enemy-name
                               get-hp
                               get-items
                               get-level
                               get-messages
                               get-position
                               get-state
                               get-xp
                               in-a-fight
                               in-fight?
                               initalize-game
                               killed-the-enemy
                               move
                               move-and-get-attacked
                               player-equips
                               player-is-equipped
                               player-is-on
                               player-has-hp
                               player-has-items
                               player-unequip
                               set-position
                               test-map-loaded
                               use-item]]
   [lum.game.cavegen :as cavegen]
   [lum.game.dataspec]
   [lum.maputil :as mu]))

(t/use-fixtures
  :each create-game)


(deftest initalize-tests
  (is (s/valid? :game/game (game-is-initialized))))

(deftest set-player-position
  (game-is-initialized)
  (set-position 25 27)
  (is (= [25 27]
         (get-in (get-state) [:player :position]))))

(deftest move-test
  (doseq [[[x y] direction end-pos] [;;Move with strings
                                     [[1 0] "left" [0 0]]
                                     [[0 0] "right" [1 0]]
                                     [[0 1] "up" [0 0]]
                                     [[0 0] "down" [0 1]]
                                     ;; Move with keys
                                     [[1 0] :left [0 0]]
                                     [[0 0] :right [1 0]]
                                     [[0 1] :up [0 0]]
                                     [[0 0] :down [0 1]]
                                     ;; don't move out of the map
                                     [[0 0] :left [0 0]]
                                     [[0 0] :up [0 0]]
                                     [[(dec mu/sizex) (dec mu/sizey)] :right [(dec mu/sizex) (dec mu/sizey)]]
                                     [[(dec mu/sizex) (dec mu/sizey)] :down [(dec mu/sizex) (dec mu/sizey)]]
                                     ;; don't move on walls
                                     [[4 1] :up [4 1]]]]
    (testing (str [x y] direction end-pos)
      (test-map-loaded x y)
      (move direction)
      (is (= end-pos (get-position))))))

(deftest load-map-test
  (testing "load a map from file"
    (test-map-loaded)
    (let [m (get-board)]
      ;; check if map is valid
      (is (s/valid? :game/board m))
      ;;(log/info (s/explain :game/board m))
      ;;only some examples
      (is (= :ground (:type (first m))))
      (is (= :wall (:type (mu/get-tile m 3 5))))
      (is (= :ground (:type (mu/get-tile m 0 2))))
      (is (= :stair-down (:type (mu/get-tile m 10 10))))
      (is (= :stair-up (:type (mu/get-tile m 10 11)))))))

(deftest player-can-stand-on-tile
  (doseq [[x y tile] [[0 0 :ground]
                      [10 10 :stair-down]
                      [10 11 :stair-up]]]
    (testing (str x y tile)
      (test-map-loaded x y)
      (is (= tile (:type (mu/get-tile (get-board) x y))))
      (is (= [x y] (get-position))))))

(deftest get-in-a-fight
  (game-is-initialized)
  (move-and-get-attacked)
  (is (in-fight?)))

(deftest kill-it
  (in-a-fight)
  ;; You kill it with the first strike
  (attack 20 3 3 1)
  (is (= 10 (get-hp)))
  (is (= 1 (get-xp)))
  (is (not (in-fight?))))

(deftest get-killed-by-enemy
  (in-a-fight)
  (attack 1 20 2 2)
  (attack 1 20 2 2)
  (attack 1 20 2 2)
  (is (= 0 (get-hp)))
  (is (game-over?)))

(deftest hit-by-enemy
  (in-a-fight)
  (attack 15 1 15 2)
  (is (= 8 (get-hp)))
  (is (= 1 (get-enemy-hp))))

(deftest enemy-always-hit-with-20
  (in-a-fight)
  (attack 1 20 2 2)
  (is (> 10 (get-hp))))

(deftest enemy-ac<roll-no-hit
  (in-a-fight)
  (attack 1 15 2 2)
  (is (> 10 (get-hp))))

(deftest enemy-ac>roll-no-hit
  (in-a-fight)
  (attack 1 2 2)
  (is (= 10 (get-hp))))

(deftest enemy-ac=roll-no-hit
  (in-a-fight)
  (attack 1 5 2)
  (is (= 10 (get-hp))))

(deftest player-always-hit-with-20
  (in-a-fight)
  (attack 20 0 1 1)
  (is (> 2 (get-enemy-hp))))

(deftest player-ac<roll-hit
  (in-a-fight)
  (attack 15 1 0)
  (is (> 2 (get-enemy-hp))))

(deftest player-ac>roll-no-hit
  (in-a-fight)
  (attack 9 2 2)
  (is (= 2 (get-enemy-hp))))

(deftest player-ac=roll-no-hit
  (in-a-fight)
  (attack 10 5 2)
  (is (= 2 (get-enemy-hp))))

(deftest armor-protects-from-damage
  (player-is-equipped :body "leather armor")
  (move-and-get-attacked "Bat")
  (attack 1 10 1)
  (is (= 10 (get-hp))))

(deftest enemy-makes-correct-damage
  (doseq [[enemy rolls hp] [["Rat" [2] 8]
                            ["Rat" [3] 9] ;;a bad input is reduced to 1
                            ["Bat" [3] 7]]]
    (testing (str enemy " " rolls " " hp)
      (initalize-game)
      (move-and-get-attacked enemy)
      (apply attack 1 15 rolls)
      (is (= hp (get-hp))))))

(deftest get-item-after-fight
  (in-a-fight)
  (killed-the-enemy)
  (is (= 1 (get (get-items) "batblood")))
  (is (= 1 (get (get-items) "batwing"))))

(deftest in-fight-with-a-rat
  (game-is-initialized)
  (move-and-get-attacked "Rat")
  (is (= "Rat" (get-enemy-name)))
  (is (clojure.string/includes? (first (get-messages)) "Rat")))

(deftest combine-items
  (player-has-items {"batblood" 2})
  (combine "batblood" "batblood")
  (is (nil? (get (get-items) "batblood")))
  (is (= 1 (get (get-items) "small healing potion"))))

(deftest combine-items-already-some-in-stock
  (player-has-items {"batblood" 2
                     "small healing potion" 1})
  (combine "batblood" "batblood")
  (is (nil? (get (get-items) "batblood")))
  (is (= 2 (get (get-items) "small healing potion"))))

(deftest combine-wrong-items
  (player-has-items {"batblood" 1
                     "batwing" 1})
  (combine "batblood" "batwing")
  (is (empty? (get-items))))

(deftest combine-items-not-in-inventory
  (player-has-items {})
  (combine "batblood" "batblood")
  (is (empty? (get-items))))

(deftest combine-items-not-enough-inventory
  (player-has-items {"batblood" 1})
  (combine "batblood" "batblood")
  (is (= {"batblood" 1} (get-items))))

(deftest apply-item
  (player-has-items {"small healing potion" 1})
  (player-has-hp 5)
  (use-item "small healing potion")
  (is (= 8 (get-hp)))
  (is (nil? (get (get-items) "small healing potion"))))

(deftest apply-item-not-in-inventory
  (player-has-hp 5)
  (use-item "small healing potion")
  (is (= 5 (get-hp))))

(deftest apply-item-when-dead
  (player-has-items {"small healing potion" 1})
  (player-has-hp 0)
  (use-item "small healing potion")
  (is (= 0 (get-hp))))

(deftest apply-item-bug-null-pointer-exception
  (player-has-items {"sword" 1})
  (use-item "sword")
  (s/valid? :game/game (get-state)))

(deftest apply-equipped-item
  (player-has-items {"sword" 1})
  (player-equips :right-hand "sword")
  (use-item "sword")
  (is (not (contains? (get-items) "sword")))
  (is (not (contains? (get-equipped-items) :right-hand))))

(deftest equip-item
  (player-has-items {"sword" 1})
  (player-equips :right-hand "sword")
  (is (= "sword" (:right-hand (get-equipped-items)))))

(deftest equip-item-which-is-not-in-stock-fails
  (player-has-items {})
  (player-equips :right-hand "sword")
  (is (not (contains? (get-equipped-items) :right-hand))))

(deftest unequip-item
  (player-is-equipped :right-hand "sword")
  (player-unequip :right-hand)
  (is (not (contains? (get-equipped-items) :right-hand))))

(deftest weapon-makes-damage
  (player-is-equipped :right-hand "sword")
  (move-and-get-attacked "Bat")
  (attack 19 5 1 1 1)
  (is (not (in-fight?))))

(deftest level-2-can-be-entered-entered
  (player-is-on :stair-down)
  (activate)
  (is (= 2 (get-level))))

(deftest level-2-not-entered-on-ground
  (player-is-on :ground)
  (activate)
  (is (= 1 (get-level))))
