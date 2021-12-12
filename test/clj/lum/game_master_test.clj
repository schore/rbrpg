(ns lum.game-master-test
  (:require
   [clojure.core.async :refer [<! <!! >! alts! alts!! chan close! go timeout]]
   [clojure.spec.alpha :as s]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [lum.game.gamelogic :as gm]
   [lum.maputil :as mu]))

(defn create-game-maser
  []
  (let [in (chan)
        out (gm/game-master in)]
    [in out]))


(defn run-game-logic
  ([commands]
   (let [[in out] (create-game-maser)]
     (run-game-logic in out commands true [])))
  ([in out commands close? accu]
   (let [responses (chan)]
     (go (doseq [command commands]
           (>! in command))
         (when close?
           (close! in)))
     (go (>! responses (loop [a accu]
                         (if-let [v (first (alts! [out
                                                   (timeout 500)]))]
                             (recur (conj a v))
                           a))))
     (let [updates (first (alts!! [responses
                                   (timeout 2000)]))]
       (close! responses)
       [in out updates]))))

(defmulti summarize-response
  (fn [_ response]
    (first response)))

(defmethod summarize-response
  :new-board
  [m [_ board]]
  (assoc m :board board))

(defmethod summarize-response
  :player-move
  [m [_ x y]]
  (assoc-in m [:player :position] [x y]))

(defmethod summarize-response
  :fight
  [m [_ fight?]]
  (assoc m :fight? fight?))

(defmethod summarize-response
  :default
  [m r]
  (log/error "Default reached " r)
  m)

(defn summarize-responses
  [responses]
  (reduce (fn [r response]
            (summarize-response r response))
          {} responses))

(defn commands-to-state [commands] (summarize-responses (nth (run-game-logic commands) 2)))

(def commands-initialized
  [[:initialize]])


(defn commands-loadmap
  [file]
  (conj commands-initialized
        [:load-map file]))


(defn commands-player-in-position
  [x y]
  (conj (commands-loadmap "docs/test.txt")
        [:set-position x y]))

(defn commands-player-move
  [startx starty direction]
  (conj (commands-player-in-position startx starty)
        [:move direction]))

(deftest calc-updates
  (testing "New board"
    (let [[action data] (first (gm/calc-updates  {:board "old val"} {:board "new val"}))]
      (is (= action :new-board))
      (is (= data "new val")))))

(deftest initalize-tests
  (testing "Initializing"
    (let [state (commands-to-state commands-initialized)]
      (is (some? (:board state)))
      (is (s/valid? :game/board (:board state)))
      (is (some? (get-in state [:player :position])))
      (is (s/valid? :game/position (get-in state [:player :position]))))))

(deftest set-player
  (let [state (commands-to-state (commands-player-in-position 50 50))]
    (testing "Set player command"
      (is (= [50 50] (get-in state [:player :position]))))))

(defn move-to-position
  [startx starty direction]
  (get-in (commands-to-state (commands-player-move startx starty direction))
          [:player :position]))

(defn commands-move-on-testmap
  [x y direction]
  (conj (commands-loadmap "docs/test.txt")
        [:set-position x y]
        [:move direction]))

(defn move-on-testmap
  [x y direction]
  (get-in (commands-to-state (commands-move-on-testmap x y direction))
          [:player :position]))

(deftest move
  (testing "move with strings"
    (is (= [0 0] (move-to-position 1 0 "left")))
    (is (= [1 0] (move-to-position 0 0 "right")))
    (is (= [0 0] (move-to-position 0 1 "up")))
    (is (= [0 1] (move-to-position 0 0 "down"))))
  (testing "normal move"
    (is (= [0 0] (move-to-position 1 0 :left)))
    (is (= [1 0] (move-to-position 0 0 :right)))
    (is (= [0 0] (move-to-position 0 1 :up)))
    (is (= [0 1] (move-to-position 0 0 :down)))
    (is (= [2 2] (move-on-testmap 2 1 :down))))
  (testing "don't move out"
    (is (= [0 0] (move-to-position 0 0 :left)))
    (is (= [0 0] (move-to-position 0 0 :up)))
    (is (= [mu/sizex mu/sizey] (move-to-position mu/sizex mu/sizey :down)))
    (is (= [mu/sizex mu/sizey] (move-to-position mu/sizex mu/sizey :right))))
  (testing "don't move on walls"
    (is (= [4 1] (move-on-testmap 4 1 :up)))
    (is (= [4 1] (move-on-testmap 4 1 :up)))
    (is (= [4 1] (move-on-testmap 4 1 :up)))
    (is (= [4 1] (move-on-testmap 4 1 :up)))))

(deftest load-map
  (testing "load a map from file"
    (let [m (:board (commands-to-state (commands-loadmap "docs/test.txt")))]
      (is (= :ground (:type (first m))))
      (is (s/valid? :game/board m))
      (is (= :wall (:type (mu/get-tile m 3 5))))
      (is (= :ground ( :type (mu/get-tile m 0 2)))))))

(defn start-fight
  []
  (with-redefs [rand (fn [] 0.98)]
    (commands-to-state (commands-move-on-testmap 1 1 :up))))

(defn attack-and-kill
  []
  (commands-to-state (conj (commands-move-on-testmap 1 1 :up)
                           [:attack])))

(deftest fight
  (testing "Starting a fight"
    (let [state (start-fight)]
      (is (:fight? state))))
  (testing "attack and kill"))
