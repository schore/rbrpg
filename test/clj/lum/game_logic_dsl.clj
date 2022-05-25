(ns lum.game-logic-dsl
  (:require [clojure.test :as t :refer [is]]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [lum.game.dataspec]
            [lum.game.gamelogic :as gamelogic]
            [lum.game.fight :as fight]
            [lum.maputil :as mu]))


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
    ;(log/info "Execute command" command)
    (swap! state (fn [state]
                   ;; This allows to cancel in case of endless loops
                   (let [f (future (gamelogic/process-actions state command))
                         result (deref f 1000 :timeout)]
                     (when (= :timeout result)
                       (future-cancel f)
                       (is false "computation takes to long"))
                     result)))
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


(defn get-state-unchecked
  []
  (exec *game* [:nop]))

(defn initalize-game
  []
  (exec *game* [:initialize]))

(defn activate
  ([]
   (exec *game* [:activate]))
  ([& rolls]
   (exec-with-rolls activate rolls)))

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
    (when (not (s/valid? :game/game state)) (log/error (s/explain :game/game state)))
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
  (let [state (get-state)]
    (get-in state [:boards (dec (:level state))])))

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

(defn get-level
  []
  (:level (get-state)))

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
   (with-redefs [fight/choose-enemy (fn [] name)]
     (exec-with-rolls #(move :down) [1 2]))))


(defn in-a-fight
  []
  (game-is-initialized)
  (move-and-get-attacked))



(defn attack
  ([]
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

(defn new-board
  []
  (exec *game* [:new-board]))

(defn get-coordinates
  [state field]
  (let [board (get-in state [:boards (dec (:level state))])]
    (mu/n-to-position (.indexOf board {:type field}))))

(defn player-is-on
  [field]
  (let [state (game-is-initialized)
        [x y] (get-coordinates state field)]
    (set-position x y)))

(defn player-is-on-level
  [level]
  (game-is-initialized)
  (while (> level (get-level))
    (player-is-on :stair-down)
    (activate)))

(defn get-tile
  []
  (let [[x y] (get-position)]
    (mu/get-tile (get-board) x y)))
