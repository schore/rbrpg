(ns lum.game-master-test
  (:require [lum.routes.websockets :as gm]
            [lum.game.dataspec :as gspec]
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

(deftest calc-updates
  (testing "New board"
    (let [[action data] (first (gm/calc-updates  {:board "old val"} {:board "new val"}))]
      (is (= action :new-board))
      (is (= data "new val")))))

(deftest game-tests
  (testing "Initializing"
    (let [state (commands-to-state game-initialized)]
      (is (some? (:board state)))
      (is (s/valid? :game/board (:board state)))
      (is (some? (get-in state [:player :position])))
      (is (s/valid? :game/position (get-in state [:player :position]))))))
