(ns lum.game.item-test
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

(deftest combine-items
  (dsl/player-has-items {"batblood" 2})
  (dsl/combine "batblood" "batblood")
  (is (nil? (get (dsl/get-items) "batblood")))
  (is (= 1 (get (dsl/get-items) "small healing potion"))))

(deftest combine-items-something-0
  (dsl/player-has-items {"batblood" 2})
  (dsl/combine-map {"batblood" 2
                    "batwing" 0})
  (is (nil? (get (dsl/get-items) "batblood")))
  (is (= 1 (get (dsl/get-items) "small healing potion"))))

(deftest remember-recipies
  (dsl/player-has-items {"batblood" 2})
  (dsl/combine "batblood" "batblood")
  (is (some #{{"batblood" 2}} (dsl/get-known-recepies))))

(deftest remember-recipies-something-is-0
  (dsl/player-has-items {"batblood" 2})
  (dsl/combine-map {"batblood" 2
                    "batwing" 0})
  (is (some #{{"batblood" 2}} (dsl/get-known-recepies))))

(deftest recipies-stored-only-once
  (dsl/player-has-items {"batblood" 4})
  (dsl/combine "batblood" "batblood")
  (dsl/combine "batblood" "batblood")
  (is (= 1 (count (dsl/get-known-recepies)))))

(deftest two-recipies-stored
  (dsl/player-has-items {"batblood" 2
                         "batwing" 1})
  (dsl/combine "batblood" "batblood")
  (dsl/combine "small healing potion" "batwing")
  (is (= 2 (count (dsl/get-known-recepies)))))

(deftest recipie-stored-only-on-success
  (dsl/player-has-items {"batblood" 1})
  (dsl/combine "batblood" "batblood")
  (is (not (some #{{"batblood" 2}} (dsl/get-known-recepies)))))

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

(deftest apply-item-can-heal
  (dsl/player-has-items {"small healing potion" 1})
  (dsl/player-has-hp 5)
  (dsl/use-item "small healing potion")
  (is (= 8 (dsl/get-hp)))
  (is (nil? (get (dsl/get-items) "small healing potion"))))

(deftest apply-item-message
  (doseq [[item message] [["small healing potion" "HP: 3"]
                          ["small mana potion" "MP: 3"]
                          ["roast beef" "Maxhp: 1"]
                          ["force scroll" "Learned spell: Force"]]]
    (testing (str item ":" message)
      (dsl/initalize-game)
      (dsl/player-has-items {item 1})
      (dsl/use-item item)
      (is (= (str "Use item: " item) (first (dsl/get-messages))))
      (is (= message (second (dsl/get-messages)))))))

(deftest apply-item-not-in-inventory
  (dsl/player-has-items {})
  (dsl/player-has-hp 5)
  (dsl/use-item "small healing potion")
  (is (= 5 (dsl/get-hp))))

(deftest apply-item-when-dead-fails
  (dsl/player-has-items {"small healing potion" 1})
  (dsl/player-has-hp 0)
  (dsl/use-item "small healing potion")
  (is (= 0 (dsl/get-hp))))

(deftest apply-item-bug-null-pointer-exception
  (dsl/player-has-items {"sword" 1})
  (dsl/use-item "sword")
  (s/valid? :game/game (dsl/get-state)))

(deftest some-items-can-not-be-applied
  (dsl/player-has-items {"sword" 1})
  (dsl/use-item "sword")
  (is (contains? (dsl/get-items) "sword")))

(deftest apply-equipped-item
  (dsl/player-has-items {"small healing potion" 1})
  (dsl/player-equips :right-hand "small healing potion")
  (dsl/use-item "small healing potion")
  (is (not (contains? (dsl/get-items) "small healing potion")))
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

(deftest check-handling-of-items
  (dsl/player-has-items {"foo" 1})
  (is (nil? (get (dsl/get-items) "foo")))
  (is (s/valid? :game/game (dsl/get-state))))

(deftest reading-a-note-gives-a-hint
  (dsl/player-has-items {"note" 1})
  (dsl/use-item "note")
  (is (some
       #{(second (dsl/get-messages))}
       db/hints)))

(deftest reading-a-note-learns-a-recipie
  (dsl/player-has-items {"note" 1})
  (dsl/use-item "note")
  (is (seq (dsl/get-known-recepies))))

(deftest using-regular-items-does-not-give-hint
  (dsl/player-has-items {"herb" 1})
  (dsl/use-item "herb")
  (is (not (some #{(first (dsl/get-messages))}
                 db/hints))))

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
