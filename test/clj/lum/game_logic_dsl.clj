(ns lum.game-logic-dsl
  (:require [clojure.test :as t :refer [is]]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [lum.game.dataspec]
            [lum.game.gamelogic :as gamelogic]
            [lum.game.fight :as fight]
            [lum.game.utilities :as util]
            [clojure.java.io :as io]
            [lum.maputil :as mu]
            [lum.game.cavegen :as cavegen]
            [lum.game.game-database :as db]
            [lum.game.utilities :as u]))

(defn delete-directory-recursive
  "Recursively delete a director ."
  [^java.io.File file]
  ;; when `file` is a directory, list its entries and call this
  ;; function with each entry. can't `recur` here as it's not a tail
  ;; position, sadly. could cause a stack overflow for many entries?
  ;; thanks to @nikolavojicic for the idea to use `run!` instead of
  ;; `doseq` :)
  (when (.isDirectory file)
    (run! delete-directory-recursive (.listFiles file)))
  ;; delete the file or directory. if it it's a file, it's easily
  ;; deletable. if it's a directory, we already have deleted all its
  ;; contents with the code above (remember?)
  (io/delete-file file))

(defn prepare-directory
  [f]
  (.mkdir (io/file "tmp"))
  (f)
  (delete-directory-recursive (io/file "tmp")))

(defn prepare-save-game
  [filename]
  (spit (str "tmp/" filename)
        (slurp (io/resource (str "savegames/" filename)))))

(defprotocol IGame
  (exec [this command])
  (get-data [this])
  (get-events [this])
  (close [this]))

(deftype Game [in out data]
  IGame

  (exec [_ command]
    (a/put! in command)
    (reset! data (first (a/alts!! [out (a/timeout 1000)]))))

  (get-data [_] @data)
  (get-events [_] nil)

  (close [_]
    (a/close! in)))

(deftype GameNoChan [state]
  IGame

  (exec [_ command]
    ;; (log/info "Execute command" command)
    (swap! state (fn [state]
                   (gamelogic/calc-commands (:data state) command))))

  (get-data [_] (:data @state))
  (get-events [_] (:events @state))

  (close [_]))

(def ^:dynamic *game*)

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
  (let [r (atom rolls)]
    (with-redefs [util/roll (fn [m]
                              (let [next-roll (first @r)]
                                (swap! r rest)
                                (if (and (some? next-roll)
                                         (<= next-roll m))
                                  next-roll
                                  (do
                                    (is (not (nil? next-roll)) "Nor more dices")
                                    (when (some? next-roll)
                                      (is (<= next-roll m) "Wrong dice"))
                                    1))))]
      (f))))

(defn get-state-unchecked
  []
  (get-data *game*))

(defn get-event
  []
  (get-events *game*))

(defn initalize-game
  []
  (exec *game* [:initialize (cavegen/get-dungeon)]))

(defn activate
  ([]
   (exec *game* [:activate]))
  ([& rolls]
   (exec-with-rolls activate rolls)))

(defn get-state
  []
  (let [state (get-state-unchecked)]
    (when (not (s/valid? :game/game state)) (log/error (s/explain :game/game state)))
    (is (s/valid? :game/game state))
    state))

(defn game-is-initialized
  []
  (when (not (s/valid? :game/game (get-state-unchecked)))
    (initalize-game))
  (get-state))

(defn load-game
  [new-sate]
  (exec *game* [:load new-sate]))

(defn save-game
  [filename]
  (exec *game* [:save filename]))

(defn combine
  [& s]
  (exec *game* [:combine (frequencies s)]))

(defn combine-map
  [m]
  (exec *game* [:combine m]))

(defn flea
  ([]
   (exec *game* [:flea]))
  ([& s]
   (exec-with-rolls flea s)))

(defn get-messages
  []
  (:messages (get-state)))

(defn get-last-message
  []
  (first (get-messages)))

(defn get-position
  []
  (rest (get-in (get-state) [:board :player-position])))

(defn get-board
  []
  (let [state (get-state)]
    (get-in state [:board :dungeons
                   (dec (get-in state [:board :player-position 0]))])))

(defn get-hp
  []
  (get-in (get-state) [:player :hp 0]))

(defn get-mp
  []
  (get-in (get-state) [:player :mp 0]))

(defn get-max-hp
  []
  (get-in (get-state) [:player :hp 1]))

(defn get-max-mp
  []
  (get-in (get-state) [:player :mp 1]))

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
  (u/get-level (get-state)))

(defn load-map
  [file]
  (exec *game* [:load-map file]))

(defn ensure-valid-map
  [state]
  (if (s/valid? :game/game state)
    state
    (recur (assoc-in state [:boards (dec (:level state))]
                     (cavegen/get-dungeon)))))

(defn set-position
  [x y]
  (game-is-initialized)
  (-> (get-state)
      (update-in [:board :player-position] (fn [[level _ _]] [level x y]))
      ensure-valid-map
      load-game))

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
   (with-redefs [fight/choose-enemy (fn [_] name)]
     (exec-with-rolls #(move :down) [1 2]))))

(defn in-a-fight
  ([]
   (in-a-fight "Bat"))
  ([enemy]
   (game-is-initialized)
   (move-and-get-attacked enemy)
   (is (in-fight?))))

(defn attack
  ([]
   (exec *game* [:attack]))
  ([& rolls]
   ;;(attack)
   (exec-with-rolls attack rolls)))

(defn cast-spell
  ([spell]
   (exec *game* [:cast-spell spell]))
  ([spell & rolls]
   (exec-with-rolls #(cast-spell spell) rolls)))

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

(defn player-know-spell
  [spell]
  (let [state (game-is-initialized)]
    (load-game (update-in state [:player :spells] #(conj % spell)))))

(defn player-has-hp
  [hp]
  (let [state (game-is-initialized)]
    (load-game (assoc-in state [:player :hp 0] hp))))

(defn player-has-mp
  [mp]
  (let [state (game-is-initialized)]
    (load-game (assoc-in state [:player :mp 0] mp))))

(defn player-equips
  [slot item]
  (exec *game* [:equip slot item]))

(defn get-equipped-items
  []
  (get-in (get-state) [:player :equipment]))

(defn player-is-equipped
  [slot item]
  (game-is-initialized)
  (player-has-items (assoc (get-items) item 1))
  (player-equips slot item)
  (is (= item (get (get-equipped-items) slot))))

(defn player-unequip
  [slot]
  (exec *game* [:unequip slot]))

(defn new-board
  []
  (exec *game* [:new-board (cavegen/get-dungeon)]))

(defn find-index
  [coll f]
  (first (keep-indexed (fn [index e] (when (f e) index)) coll)))

(defn get-coordinates
  [state field]
  (let [board (get-in state [:board :dungeons (dec (u/get-level state))])]
    (mu/n-to-position (find-index board #(= field (:type %))))))

(defn get-current-field
  []
  (let [[x y] (u/get-position (get-state))
        board (get-board)]
    (mu/get-tile board x y)))

(defn player-is-on
  [field]
  (let [state (game-is-initialized)
        [x y] (get-coordinates state field)]
    (set-position x y))
  (is (= field (:type (get-current-field)))))

(defn items-on-ground
  [items]
  (let [state (game-is-initialized)
        level (dec (u/get-level state))
        [x y] (u/get-position state)]
    (load-game (assoc-in state [:board :dungeons level (mu/position-to-n x y) :items] items))))

(defn enter-level
  ([level]
   (enter-level level (cavegen/get-dungeon)))
  ([level map]
   (game-is-initialized)
   (exec *game* [:enter-unknown-level level map])))

(defn player-is-on-level
  [level]
  (game-is-initialized)
  (doseq [i (range 2 (inc level))]
    (enter-level i))
  (is (= level (get-level)))
  (is (not (in-fight?))))

(defn get-tile
  ([]
   (let [[x y] (get-position)]
     (mu/get-tile (get-board) x y)))
  ([x y]
   (mu/get-tile (get-board) x y)))

(defn player-has-xp
  [xp]
  (let [state (game-is-initialized)]
    (load-game (assoc-in state [:player :xp] xp))))

(defn get-one-xp
  []
  (in-a-fight)
  (killed-the-enemy))

(defn get-spells
  []
  (get-in (get-state) [:player :spells]))

(defn field-visible?
  [x y]
  (:visible? (mu/get-tile (get-board) x y)))

(defn remove-visible
  [b]
  (map #(dissoc % :visible?) b))

(defn same-boards?
  [b1 b2]
  (= (remove-visible b1) (remove-visible b2)))

(defn board-completly-visible?
  []
  (let [board (get-board)]
    (reduce (fn [a tile]
              (and (:visible? tile) a))
            true board)))

(defn player-is-on-special-map
  []
  (player-is-on-level 4)
  (enter-level 5
               (gamelogic/load-special-map  (get-in db/special-maps [5 :map]) 5)))

(defn player-steps-on-message-trigger
  []
  (set-position 10 10)
  (move :right))

(defn player-steps-on-fight-trigger
  []
  (set-position 10 11)
  (move :right))

(defn get-known-recepies
  []
  (-> (get-state) :player :recepies))
