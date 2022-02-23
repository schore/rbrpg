(ns lum.game.gamelogic-test
  (:require
   [clojure.core.async :as a :refer [<! <!! >! alts! alts!! chan close! go timeout]]
   [clojure.spec.alpha :as s]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.string]
   [lum.game.cavegen :as cavegen]
   [clojure.tools.logging :as log]
   [lum.game.gamelogic :as gm]
   [lum.game.dataspec]
   [lum.maputil :as mu]))

(defn create-game-maser
  []
  (let [in (chan)
        out (gm/game-master in)]
    [in out]))

(defprotocol IGame
  (exec [this command])
  (close [this]))

(deftype Game [in out]
  IGame

  (exec [_ command ]
      (log/info "Execute command" command)
      (a/>!! in command)
      (a/<!! out))

  (close [_]
    (log/info "Close Test")
    (a/close! in)))

(def ^:dynamic *game*)

(defn create-game
  ([]
   (let [[in out] (create-game-maser)]
     (Game. in out)))
  ([f]
   (binding [*game* (create-game)]
     (f)
     (close *game*))))

(t/use-fixtures
  :each create-game)


(defn game-is-initialized
  []
  (exec *game* [:initialize]))

(defn game-loaded
  [new-sate]
  (exec *game* [:load new-sate]))

(defn set-position
  [x y]
  (exec *game* [:set-position x y]))

(defn get-state
  []
  (exec *game* [:nop]))

(defn get-position
  []
  (get-in (get-state) [:player :position]))

(defn get-board
  []
  (:board (get-state)))

(defn get-hp
  []
  (get-in (get-state) [:player :hp 0]))

(defn get-enemy-hp
  []
  (get-in (get-state) [:fight :enemy :hp 0]))

(defn get-xp
  []
  (get-in (get-state) [:player :xp]))

(defn in-fight?
  []
  (some? (:fight (get-state))))

(defn game-over?
  []
  (= 0 (get-hp)))

(defn load-map
  [file]
  (exec *game* [:load-map file]))

(defn test-map-loaded
  ([]
   (game-is-initialized)
   (load-map "docs/test.txt"))
  ([x y]
   (test-map-loaded)
   (set-position x y)))


(defn move
  [dir]
  (exec *game* [:move dir]))

(defn move-and-get-attacked
  [dir]
  (with-redefs [rand (fn [] 0.98)]
    (move dir)))


(defn in-a-fight
  []
  (game-is-initialized)
  (move-and-get-attacked :up))

(defn exec-with-rolls
  [f rolls]
  (let [r (atom (map dec rolls))]
    (with-redefs [rand-int (fn [_]
                             (let [next-roll (first @r)]
                               (swap! r rest)
                               next-roll))]
      (f))))

(defn attack
  ([]
   (log/info "attack")
   (exec *game* [:attack]))
  ([& rolls]
   ;;(attack)
   (exec-with-rolls attack rolls)
   ))



(deftest initalize-tests
  (is (s/valid? :game/game (game-is-initialized))))


(deftest load-game
  (testing "Load a game"
    (let [game-state {:board (cavegen/get-dungeon)
                      :messages '("")
                      :player {:position [12 12]
                               :ac 5
                               :xp 0
                               :hp [10 10]
                               :mp [3 3]}}]
      (game-is-initialized)
      (game-loaded game-state)
      (is (= game-state (get-state))))))


(deftest load-of-invalide-game-data-prevented
  (game-is-initialized)
  (game-loaded {})
  (is (s/valid? :game/game (get-state))))


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
                                     [[4 1] :up [4 1]]
                                     ]]
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
      ;;only some examples
      (is (= :ground (:type (first m))))
      (is (= :wall (:type (mu/get-tile m 3 5))))
      (is (= :ground (:type (mu/get-tile m 0 2)))))))



(deftest get-in-a-fight
  (game-is-initialized)
  (move-and-get-attacked :down)
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
  (attack 1 20 5 5)
  (is (= 0 (get-hp)))
  (is (game-over?)))

(deftest hit-by-enemy
  (in-a-fight)
  (attack 15 1 15 2)
  (is (= 8 (get-hp)))
  (is (= 1 (get-enemy-hp))))
