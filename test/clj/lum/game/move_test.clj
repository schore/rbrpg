(ns lum.game.move-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string]
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game-logic-dsl :as dsl]
   [lum.game.dataspec]
   [lum.maputil :as mu]))

(t/use-fixtures
  :each dsl/create-game)

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

(deftest player-can-stand-on-tile
  (doseq [[x y tile] [[0 0 :ground]
                      [21 19 :stair-down]
                      [22 19 :stair-up]]]
    (testing (str x y tile)
      (dsl/test-map-loaded x y)
      (is (= tile (:type (mu/get-tile (dsl/get-board) x y))))
      (is (= [x y] (dsl/get-position))))))
(deftest on-stairs-up-when-entering-next-level
  (dsl/enter-level 2)
  (is (= :stair-up (:type (dsl/get-tile)))))

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
    (is (dsl/same-boards? board (dsl/get-board)))))

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
