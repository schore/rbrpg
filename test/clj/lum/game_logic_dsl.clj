(ns lum.game-logic-dsl
  (:require [clojure.test :as t :refer [is]]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [lum.game.dataspec]
            [lum.game.gamelogic :as gamelogic]))


(defn create-game-maser
  []
  (let [in (a/chan)
        out (gamelogic/game-master in)]
    [in out]))

(defprotocol IGame
  (exec [this command])
  (close [this]))

(deftype Game [in out]
  IGame

  (exec [_ command ]
      (log/info "Execute command" command)
    (a/put! in command)
    (first (a/alts!! [out (a/timeout 1000)])))

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

(defn get-state-unchecked
  []
  (exec *game* [:nop]))

(defn initalize-game
  []
  (exec *game* [:initialize]))

(defn game-is-initialized
  []
  (let [state (get-state-unchecked)]
    (if (not (s/valid? :game/game state))
      (initalize-game)
      state)))

(defn game-loaded
  [new-sate]
  (exec *game* [:load new-sate]))

(defn set-position
  [x y]
  (exec *game* [:set-position x y]))

(defn get-state
  []
  (let [state (exec *game* [:nop])]
    (is (s/valid? :game/game state))
    state))

(defn combine
  [& s]
  (exec *game* [:combine (frequencies s)]))

(defn get-messages
  []
  (:messages (get-state)))

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

(defn get-enemy-name
  []
  (get-in (get-state) [:fight :enemy :name]))

(defn get-xp
  []
  (get-in (get-state) [:player :xp]))

(defn get-items
  []
  (get-in (get-state) [:player :items]))

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
   (initalize-game)
   (load-map "docs/test.txt"))
  ([x y]
   (test-map-loaded)
   (set-position x y)))


(defn move
  [dir]
  (exec *game* [:move dir]))

(defn move-and-get-attacked
  ([] (move-and-get-attacked "Bat"))
  ([name]
   (with-redefs [rand (fn [] 0.98)
                 gamelogic/choose-enemy (fn [] name)]
     (move :down))))


(defn in-a-fight
  []
  (game-is-initialized)
  (move-and-get-attacked))

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

(defn use-item
  [item]
  (exec *game* [:use-item item]))


(defn killed-the-enemy
  []
  (attack 20 2 1 1 1))

(defn player-has-items
  [items]
  (let [state (game-is-initialized)]
    (game-loaded (assoc-in state [:player :items] items))))

(defn player-has-hp
  [hp]
  (let [state (game-is-initialized)]
    (log/info (:player state))
    (game-loaded (assoc-in state [:player :hp 0] hp))))

