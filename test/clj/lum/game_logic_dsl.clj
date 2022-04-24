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

(deftype GameNoChan [state]
  IGame

  (exec [_ command]
    (swap! state #(gamelogic/process-actions % command))
    @state)

  (close [_]))

(def ^:dynamic *game*)

(defn create-game-with-chan
  ([]
   (let [[in out] (create-game-maser)]
     (Game. in out)))
  ([f]
   (binding [*game* (create-game-with-chan)]
     (f)
     (close *game*))))


(defn create-game-without-chan
  [f]
  (binding [*game* (GameNoChan. (atom {}))]
    (f)
    (close *game*)))

(defn create-game
  [f]
  (create-game-without-chan f))

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

(defn load-game
  [new-sate]
  (exec *game* [:load new-sate]))

(defn save-game
  [filename]
  (exec *game* [:save filename]))

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
    (with-redefs [rand-int (fn [m]
                             (let [next-roll (first @r)]
                               (swap! r rest)
                               (if (< next-roll m)
                                 next-roll
                                 (do (log/error "You selected the wrong dice")
                                     0))))]
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
    (load-game (assoc-in state [:player :items] items))))

(defn player-has-hp
  [hp]
  (let [state (game-is-initialized)]
    (log/info (:player state))
    (load-game (assoc-in state [:player :hp 0] hp))))

(defn player-equips
  [slot item]
  (exec *game* [:equip slot item]))

(defn get-equipped-items
  []
  (get-in (get-state) [:player :equipment]))

(defn player-is-equipped
  [slot item]
  (player-has-items {item 1})
  (player-equips slot item)
  (is (= item (get (get-equipped-items) slot))))

(defn player-unequip
  [slot]
  (exec *game* [:unequip slot]))
