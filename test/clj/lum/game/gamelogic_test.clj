(ns lum.game.gamelogic-test
  (:require
   [clojure.core.async :refer [<! <!! >! alts! alts!! chan close! go timeout]]
   [clojure.spec.alpha :as s]
   [clojure.test :as t :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [lum.game.gamelogic :as gm]
   [lum.game.dataspec]
   [lum.maputil :as mu]
   [clojure.core.async :as a]))

(defn create-game-maser
  []
  (let [in (chan)
        out (gm/game-master in)]
    [in out]))

(defn run-game-logic
  ([commands]
   (let [[in out] (create-game-maser)]
     (run-game-logic commands true [] in out)))
  ([commands close? accu in out]
;;   (log/info commands accu)
   (let [responses (chan)
         processing-done (chan)]
     (go (doseq [command commands]
           (>! in command))
         (>! in [:nop])
         (close! processing-done)
         (when close?
           (close! in)))
     (go (>! responses (loop [a accu]
                         (if-let [v (first (alts! [out
                                                   processing-done
                                                   (timeout 500)]))]
                           (do
                             ;;(log/info a v)
                             (recur (conj a v)))
                           a))))
     (let [updates (<!! responses)]
       (close! responses)
 ;;      (log/info updates)
       updates))))

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
  [m [_ fight]]
  (if (some? fight)
    (assoc m :fight fight)
    (dissoc m :fight)))

(defmethod summarize-response
  :hp
  [m [_ current max]]
  (assoc-in m [:player :hp] [current max]))

(defmethod summarize-response
  :mp
  [m [_ current max]]
  (assoc-in m [:player :mp] [current max]))

(defmethod summarize-response
  :xp
  [m [_ xp]]
  (assoc-in m [:player :xp] xp))

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

(defn commands-to-state [commands] (summarize-responses (run-game-logic commands)))

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


(deftest initalize-tests
  (testing "Initializing"
    (let [state (commands-to-state commands-initialized)]
      (is (some? (:board state)))
      (is (s/valid? :game/board (:board state)))
      (is (some? (get-in state [:player :position])))
      (is (s/valid? :game/position (get-in state [:player :position])))
      (is (s/valid? :game/game state)))))

(deftest set-player
  (let [state (commands-to-state (commands-player-in-position 25 25))]
    (testing "Set player command"
      (is (= [25 25] (get-in state [:player :position]))))))

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
    (is (= [(dec mu/sizex) (dec mu/sizey)] (move-to-position (dec mu/sizex) (dec mu/sizey) :down)))
    (is (= [(dec mu/sizex) (dec mu/sizey)] (move-to-position (dec mu/sizex) (dec mu/sizey) :right))))
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
      (is (= :ground (:type (mu/get-tile m 0 2)))))))

(defn start-fight
  []
  (with-redefs [rand (fn [] 0.98)]
    (commands-to-state (commands-move-on-testmap 1 1 :up))))
(defn start-fight-and-kill
  ([]
   (let [[in out] (create-game-maser)
         a (start-fight-and-kill in out
                                 (run-game-logic [[:initialize]] false [] in out))]
     (a/close! in)
     (summarize-responses a)))
  ([in out a]
   (with-redefs [rand (fn [] 0.98)
                 gm/player-attacks (fn [_]  ["Beat" [{:target :enemy
                                                       :stat :hp
                                                       :n -1}]])]
     (run-game-logic (concat [[:move :up]]
                             [[:attack] [:attack]])
                     false a in out))))

(defn fight-until-game-over
  []
  (let [[in out] (create-game-maser)
        a (run-game-logic [[:initialize]] false [] in out)
        a (loop [i 0 a a]
            (if (< i 10)
              (recur (inc i) (start-fight-and-kill in out a))
              a))]
    (a/close! in)
    (summarize-responses a)))


(deftest fight
  (testing "Starting a fight"
    (let [state (start-fight)]
      (is (some? (:fight state)))))
  (testing "attack and kill"
    (let [state (start-fight-and-kill)]
      (is (not (contains? state :fight)))
      (is (= 9 (get-in state [:player :hp 0])))
      (is (= 1 (get-in state [:player :xp])))))
  (testing "Fight until you die"
    (let [state (fight-until-game-over)]
      (is (= 0 (get-in state [:player :hp 0]))))))
