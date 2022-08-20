(ns lum.game.gamelogic-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string]
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game-logic-dsl :as dsl]
   [lum.game.dataspec]
   [lum.maputil :as mu]
   [lum.game.game-database :as db]))

(t/use-fixtures
  :each dsl/create-game)

(deftest initalize-tests
  (is (s/valid? :game/game (dsl/game-is-initialized))))

(deftest set-player-position
  (dsl/game-is-initialized)
  (dsl/set-position 25 27)
  (is (= [25 27]
         (get-in (dsl/get-state) [:player :position]))))

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
      (dsl/test-map-loaded x y)
      (dsl/move direction)
      (is (= end-pos (dsl/get-position))))))

(deftest load-map-test
  (testing "load a map from file"
    (dsl/test-map-loaded)
    (let [m (dsl/get-board)]
      ;; check if map is valid
      (is (s/valid? :game/board m))
      ;;(log/info (s/explain :game/board m))
      ;;only some examples
      (is (= :ground (:type (first m))))
      (is (= :wall (:type (mu/get-tile m 3 5))))
      (is (= :ground (:type (mu/get-tile m 0 2))))
      (is (= :stair-down (:type (mu/get-tile m 10 10))))
      (is (= :stair-up (:type (mu/get-tile m 10 11)))))))

(deftest create-new-board
  (dsl/game-is-initialized)
  (let [old-board (dsl/get-board)]
    (dsl/new-board)
    (is (s/valid? :game/game (dsl/get-state)))
    (is (not= old-board
              (dsl/get-board)))))

(deftest player-can-stand-on-tile
  (doseq [[x y tile] [[0 0 :ground]
                      [10 10 :stair-down]
                      [10 11 :stair-up]]]
    (testing (str x y tile)
      (dsl/test-map-loaded x y)
      (is (= tile (:type (mu/get-tile (dsl/get-board) x y))))
      (is (= [x y] (dsl/get-position))))))

(deftest get-in-a-fight
  (dsl/game-is-initialized)
  (dsl/move-and-get-attacked)
  (is (dsl/in-fight?)))

(deftest kill-it
  (dsl/in-a-fight)
  ;; You kill it with the first strike
  (dsl/attack 20 2 2 1 1)
  (is (= 10 (dsl/get-hp)))
  (is (= 1 (dsl/get-xp)))
  (is (not (dsl/in-fight?))))

(deftest get-killed-by-enemy
  (dsl/in-a-fight)
  (dsl/attack 1 20 2 2)
  (dsl/attack 1 20 2 2)
  (dsl/attack 1 20 2 2)
  (is (= 0 (dsl/get-hp)))
  (is (dsl/game-over?)))

(deftest hit-by-enemy
  (dsl/in-a-fight)
  (dsl/attack 15 1 15 2)
  (is (= 8 (dsl/get-hp)))
  (is (= 1 (dsl/get-enemy-hp))))

(deftest enemy-always-hit-with-20
  (dsl/in-a-fight)
  (dsl/attack 1 20 2 2)
  (is (> 10 (dsl/get-hp))))

(deftest enemy-ac<roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 1 15 2 2)
  (is (> 10 (dsl/get-hp))))

(deftest enemy-ac>roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 1 2 2)
  (is (= 10 (dsl/get-hp))))

(deftest enemy-ac=roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 1 5 2)
  (is (= 10 (dsl/get-hp))))

(deftest player-always-hit-with-20
  (dsl/in-a-fight)
  (dsl/attack 20 0 1 1)
  (is (> 2 (dsl/get-enemy-hp))))

(deftest player-ac<roll-hit
  (dsl/in-a-fight)
  (dsl/attack 15 1 0)
  (is (> 2 (dsl/get-enemy-hp))))

(deftest player-ac>roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 9 2 2)
  (is (= 2 (dsl/get-enemy-hp))))

(deftest player-ac=roll-no-hit
  (dsl/in-a-fight)
  (dsl/attack 10 5 2)
  (is (= 2 (dsl/get-enemy-hp))))

(deftest armor-protects-from-damage
  (dsl/player-is-equipped :body "leather armor")
  (dsl/move-and-get-attacked "Bat")
  (dsl/attack 1 10 1)
  (is (= 10 (dsl/get-hp))))

(deftest enemy-makes-correct-damage
  (doseq [[enemy rolls hp] [["Rat" [2] 8]
                            ["Bat" [3] 7]]]
    (testing (str enemy " " rolls " " hp)
      (dsl/initalize-game)
      (dsl/move-and-get-attacked enemy)
      (apply dsl/attack 1 15 rolls)
      (is (= hp (dsl/get-hp))))))

(deftest get-item-after-fight
  (dsl/in-a-fight)
  (dsl/killed-the-enemy)
  (is (= 1 (get (dsl/get-items) "batblood")))
  (is (= 1 (get (dsl/get-items) "batwing"))))

(deftest in-fight-with-a-rat
  (dsl/game-is-initialized)
  (dsl/move-and-get-attacked "Rat")
  (is (= "Rat" (dsl/get-enemy-name)))
  (is (clojure.string/includes? (first (dsl/get-messages)) "Rat")))

(deftest combine-items
  (dsl/player-has-items {"batblood" 2})
  (dsl/combine "batblood" "batblood")
  (is (nil? (get (dsl/get-items) "batblood")))
  (is (= 1 (get (dsl/get-items) "small healing potion"))))

(deftest combine-items-already-some-in-stock
  (dsl/player-has-items {"batblood" 2
                         "small healing potion" 1})
  (dsl/combine "batblood" "batblood")
  (is (nil? (get (dsl/get-items) "batblood")))
  (is (= 2 (get (dsl/get-items) "small healing potion"))))

(deftest combine-wrong-items
  (dsl/player-has-items {"batblood" 1
                         "batwing" 1})
  (dsl/combine "batblood" "batwing")
  (is (empty? (dsl/get-items))))

(deftest combine-items-not-in-inventory
  (dsl/player-has-items {})
  (dsl/combine "batblood" "batblood")
  (is (empty? (dsl/get-items))))

(deftest combine-items-not-enough-inventory
  (dsl/player-has-items {"batblood" 1})
  (dsl/combine "batblood" "batblood")
  (is (= {"batblood" 1} (dsl/get-items))))

(deftest apply-item
  (dsl/player-has-items {"small healing potion" 1})
  (dsl/player-has-hp 5)
  (dsl/use-item "small healing potion")
  (is (= 8 (dsl/get-hp)))
  (is (nil? (get (dsl/get-items) "small healing potion"))))

(deftest apply-item-not-in-inventory
  (dsl/player-has-items {})
  (dsl/player-has-hp 5)
  (dsl/use-item "small healing potion")
  (is (= 5 (dsl/get-hp))))

(deftest apply-item-when-dead
  (dsl/player-has-items {"small healing potion" 1})
  (dsl/player-has-hp 0)
  (dsl/use-item "small healing potion")
  (is (= 0 (dsl/get-hp))))

(deftest apply-item-bug-null-pointer-exception
  (dsl/player-has-items {"sword" 1})
  (dsl/use-item "sword")
  (s/valid? :game/game (dsl/get-state)))

(deftest apply-equipped-item
  (dsl/player-has-items {"sword" 1})
  (dsl/player-equips :right-hand "sword")
  (dsl/use-item "sword")
  (is (not (contains? (dsl/get-items) "sword")))
  (is (not (contains? (dsl/get-equipped-items) :right-hand))))

(deftest combine-equipped-items
  (dsl/player-has-items {"sword" 1 "wooden stick" 1})
  (dsl/player-equips :right-hand "sword")
  (dsl/combine "sword" "wooden stick")
  (is (not (contains? (dsl/get-equipped-items) :right-hand))))

(deftest equip-item
  (doseq [[item slot] [["sword" :right-hand]
                       ["pickaxe" :right-hand]
                       ["wooden stick" :right-hand]
                       ["leather armor" :body]]]
    (testing (str "Item can be equipped" item slot)
      (dsl/initalize-game)
      (dsl/player-has-items {item 1})
      (dsl/player-equips slot item)
      (is (= item (slot (dsl/get-equipped-items)))))))

(deftest equip-item-which-is-not-in-stock-fails
  (dsl/player-has-items {})
  (dsl/player-equips :right-hand "sword")
  (is (not (contains? (dsl/get-equipped-items) :right-hand))))

(deftest unequip-item
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/player-unequip :right-hand)
  (is (not (contains? (dsl/get-equipped-items) :right-hand))))

(deftest weapon-makes-damage
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/move-and-get-attacked "Bat")
  (dsl/attack 19 5 1 1 1)
  (is (not (dsl/in-fight?))))

(deftest level-2-can-be-entered-entered
  (dsl/player-is-on :stair-down)
  (dsl/activate)
  (is (= 2 (dsl/get-level))))

(deftest on-stairs-up-when-entering-next-level
  (dsl/player-is-on :stair-down)
  (dsl/activate)
  (is (= :stair-up (:type (dsl/get-tile)))))

(deftest level-2-not-entered-on-ground
  (dsl/player-is-on :ground)
  (dsl/activate)
  (is (= 1 (dsl/get-level))))

(deftest can-go-one-level-up
  (dsl/player-is-on-level 2)
  (dsl/player-is-on :stair-up)
  (dsl/activate)
  (is (= 1  (dsl/get-level))))

(deftest on-stairs-down-when-entering-previous-level
  (dsl/player-is-on-level 2)
  (dsl/player-is-on :stair-up)
  (dsl/activate)
  (is (= :stair-down (:type (dsl/get-tile)))))

(deftest enter-same-levels-when-going-up-and-down
  (dsl/player-is-on-level 2)
  (dsl/player-is-on :stair-up)
  (let [board (dsl/get-board)]
    (dsl/activate);;go-up
    (dsl/activate);;go-down
    (is (= board (dsl/get-board)))))

(deftest create-only-necessary-boards-when-going-down
  (dsl/player-is-on-level 2)
  (dsl/player-is-on :stair-up)
  (dsl/activate);;go-up
  (dsl/activate);;go-down
  (is (= 2 (count (:boards (dsl/get-state))))))

(deftest going-up-on-level-one-does-not-crash
  (dsl/player-is-on-level 1)
  (dsl/player-is-on :stair-up)
  (dsl/activate)
  (is (s/valid? :game/game (dsl/get-state))))

(deftest picking-on-empty-tile-gives-nothing
  (dsl/player-is-on :ground)
  (dsl/player-has-items {})
  (dsl/activate)
  (is (empty? (dsl/get-items))))

(deftest activating-a-tile-can-give-item
  (dsl/player-is-on :ground)
  (dsl/player-has-items {})
  (dsl/items-on-ground {"herb" 2})
  (dsl/activate 20 20)
  (is (= 2 (get (dsl/get-items) "herb"))))

(deftest pick-up-only-once
  (dsl/player-is-on :ground)
  (dsl/player-has-items {})
  (dsl/items-on-ground {"herb" 1})
  (dsl/activate 20 20)
  (dsl/activate 20 20)
  (is (= 1 (get (dsl/get-items) "herb"))))

(deftest get-a-message-when-picking-up
  (dsl/player-is-on :ground)
  (dsl/player-has-items {})
  (dsl/items-on-ground {"herb" 2})
  (dsl/activate 20 20)
  (is (clojure.string/includes? (first (dsl/get-messages)) "found")))

(deftest check-handling-of-items
  (dsl/player-has-items {"foo" 1})
  (is (nil? (get (dsl/get-items) "foo")))
  (is (s/valid? :game/game (dsl/get-state))))

(deftest enter-fight-after-activation
  (dsl/game-is-initialized)
  (dsl/activate 1 1 1 1)
  (is (dsl/in-fight?)))

(deftest dig-a-hole
  (dsl/test-map-loaded 1 4)
  (dsl/player-is-equipped :right-hand "pickaxe")
  (dsl/move :up)
  (is (= [1 3] (dsl/get-position))))

(deftest you-can-not-dig-with-a-sword
  (dsl/test-map-loaded 1 4)
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/move :up)
  (is (= [1 4] (dsl/get-position))))

(deftest player-can-increase-level
  (dsl/player-has-xp 299)
  (dsl/get-one-xp)
  (is (= 16 (dsl/get-max-hp))))

(deftest player-increase-mp-by-leveling-up
  (dsl/player-has-xp 299)
  (dsl/get-one-xp)
  (is (= 6 (dsl/get-max-mp))))

(deftest player-hp-does-not-increase-on-regular-xp-updates
  (dsl/player-has-xp 260)
  (dsl/get-one-xp)
  (is (= 10 (dsl/get-max-hp))))

(deftest bandit-gives-aromor-if-lucky
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/in-a-fight "Bandit")
  (dsl/attack 20 6 6 15 1 1 1)
  (is (contains? (dsl/get-items) "leather armor")))

(deftest bandit-gives-no-aromor-if-unlucky
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/in-a-fight "Bandit")
  (dsl/attack 20 6 6 1 1 1 1)
  (is (not (contains? (dsl/get-items) "leather armor"))))

(deftest message-about-looted-items
  (dsl/player-is-equipped :right-hand "sword")
  (dsl/in-a-fight "Bandit")
  (dsl/attack 20 6 6 15 1 1 1)
  (is (clojure.string/includes? (first (dsl/get-messages)) "looted")))

(deftest reading-a-note-gives-a-hint
  (dsl/player-has-items {"note" 1})
  (dsl/use-item "note")
  (is (some
       #{(first (dsl/get-messages))}
       db/hints)))

(deftest using-regular-items-does-not-give-hint
  (dsl/player-has-items {"herb" 1})
  (dsl/use-item "herb")
  (is (not (some #{(first (dsl/get-messages))}
                 db/hints))))

(deftest in-a-fight-fleeing-successfull
  (dsl/in-a-fight "Bandit")
  (dsl/flea 20)
  (is (not (dsl/in-fight?))))

(deftest fleeing-unsuscessfull
  (dsl/in-a-fight "Bandit")
  (dsl/flea 1 1)
  (is (dsl/in-fight?)))

(deftest enemy-attacks-when-fleeing-fails
  (dsl/in-a-fight "Bandit")
  (dsl/flea 1 20 1 1 1 1)
  (is (< (dsl/get-hp) 10)))

(deftest roast-beef-add-max-hp
  (dsl/player-has-items {"roast beef" 1})
  (dsl/use-item "roast beef")
  (is (= 11 (dsl/get-hp)))
  (is (= 11 (dsl/get-max-hp))))

(deftest increasing-maxhp-also-increases-hp
  (dsl/player-has-hp 9)
  (dsl/player-has-items {"roast beef" 1})
  (dsl/use-item "roast beef")
  (is (= 10 (dsl/get-hp))))

(deftest magic-can-end-fights-with-damage
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands" 1 1 3 1 1 1 1)
  (is (not (dsl/in-fight?))))

(deftest magic-makes-damage-according-to-rolls
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands" 1 1 1)
  (is (= 2 (dsl/get-enemy-hp))))

(deftest magic-burns-hp
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands")
  (is (= 0 (dsl/get-mp))))

(deftest can-not-cast-unknown-spells
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Magic missle")
  (is (= 5 (dsl/get-enemy-hp))))

(deftest can-not-cast-with-insufficen-mana
  (dsl/player-has-mp 0)
  (dsl/in-a-fight "Bandit")
  (dsl/cast-spell "Burning Hands")
  (is (dsl/in-fight?)))

(deftest learning-new-spells-from-items
  (dsl/player-has-items {"Force scroll" 1})
  (dsl/use-item "Force scroll")
  (is (contains? (dsl/get-spells) "Force")))

(deftest restore-mp
  (dsl/player-has-mp 0)
  (dsl/player-has-items {"small mana potion" 1})
  (dsl/use-item "small mana potion")
  (is (= 3 (dsl/get-mp))))

(deftest healing-spell-during-fight
  (dsl/in-a-fight)
  (dsl/player-has-hp 1)
  (dsl/player-know-spell "Healing")
  (dsl/cast-spell "Healing")
  (is (> (dsl/get-hp) 1)))

(deftest healing-spell-during-moving
  (dsl/player-has-hp 1)
  (dsl/player-know-spell "Healing")
  (dsl/cast-spell "Healing")
  (is (> (dsl/get-hp) 1)))

(deftest attack-spell-not-possible
  (dsl/game-is-initialized)
  (dsl/cast-spell "Burning Hands")
  (is (= 3 (dsl/get-mp))))
