(ns lum.game-master-test
  (:require [lum.routes.websockets :as gm]
            [lum.maputil :as mu]
            [clojure.spec.alpha :as s]
            [clojure.core.async :refer [>!! <!! chan alts!! timeout put! take! go >! <! close!]]
            [clojure.test :as t :refer [testing deftest is]]
            [clojure.tools.logging :as log]))

(defn create-game-maser
  []
  (let [in (chan)
        out (gm/game-master in)]
    [in out]))

(defn run-gamle-logic
  [commands]
  (let [[in out] (create-game-maser)
        responses (chan)]
    (go (doseq [command commands]
          (>! in command))
        (close! in))
    (go (>! responses (loop [a []]
                        (if-let [v (<! out)]
                          (recur (conj a v))
                          a))))
    (first (alts!! [responses
                    (timeout 2000)]))))

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
  :default
  [m r]
  (log/error "Default reached " r)
  m)

(defn summarize-responses
  [responses]
  (reduce (fn [r response]
            (summarize-response r response))
          {} responses))

(defn commands-to-state [commands] (summarize-responses (run-gamle-logic commands)))

(def game-initialized
  [[:initialize]])

(defn player-in-position
  [x y]
  (conj game-initialized
        [:set-position x y]))

(defn player-move
  [startx starty direction]
  (conj (player-in-position startx starty)
        [:move direction]))

(deftest calc-updates
  (testing "New board"
    (let [[action data] (first (gm/calc-updates  {:board "old val"} {:board "new val"}))]
      (is (= action :new-board))
      (is (= data "new val")))))

(deftest initalize-tests
  (testing "Initializing"
    (let [state (commands-to-state game-initialized)]
      (is (some? (:board state)))
      (is (s/valid? :game/board (:board state)))
      (is (some? (get-in state [:player :position])))
      (is (s/valid? :game/position (get-in state [:player :position]))))))

(deftest set-player
  (let [state (commands-to-state (player-in-position 50 50))]
    (testing "Set player command"
      (is (= [50 50] (get-in state [:player :position]))))))

(defn move-to-position
  [startx starty direction]
  (get-in (commands-to-state (player-move startx starty direction))
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
    (is (= [0 1] (move-to-position 0 0 :down))))
  (testing "don't move out"
    (is (= [0 0] (move-to-position 0 0 :left)))
    (is (= [0 0] (move-to-position 0 0 :up)))
    (is (= [mu/sizex mu/sizey] (move-to-position mu/sizex mu/sizey :down)))
    (is (= [mu/sizex mu/sizey] (move-to-position mu/sizex mu/sizey :right)))))
