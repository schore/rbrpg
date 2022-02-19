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
  (exec [_ command]
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



;; (defn run-game-logic
;;   ([commands]
;;    (let [[in out] (create-game-maser)]
;;      (run-game-logic commands true [] in out)))
;;   ([commands close? accu in out]
;; ;;   (log/info commands accu)
;;    (let [responses (chan)
;;          processing-done (chan)]
;;      (go (doseq [command commands]
;;            (>! in command))
;;          (>! in [:nop])
;;          (close! processing-done)
;;          (when close?
;;            (close! in)))
;;      (go (>! responses (loop [a accu]
;;                          (if-let [v (first (alts! [out
;;                                                    processing-done
;;                                                    (timeout 500)]))]
;;                            (do
;;                              ;;(log/info a v)
;;                              (recur (conj a v)))
;;                            a))))
;;      (let [updates (<!! responses)]
;;        (close! responses)
;;  ;;      (log/info updates)
;;        updates))))

;; (defn summarize-responses
;;   [responses]
;;   (doseq [response responses]
;;     (is (s/valid? :game/game response)))
;;   (last responses))

;; (defn commands-to-state [commands] (summarize-responses (run-game-logic commands)))

;; (def commands-initialized
;;   [[:initialize]])

;; (defn commands-loadmap
;;   [file]
;;   (conj commands-initialized
;;         [:load-map file]))

;; (defn commands-player-in-position
;;   [x y]
;;   (conj (commands-loadmap "docs/test.txt")
;;         [:set-position x y]))

;; (defn commands-player-move
;;   [startx starty direction]
;;   (conj (commands-player-in-position startx starty)
;;         [:move direction]))
;;
;;
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


;; (defn move-to-position
;;   [startx starty direction]
;;   (get-in (commands-to-state (commands-player-move startx starty direction))
;;           [:player :position]))

;; (defn commands-move-on-testmap
;;   [x y direction]
;;   (conj (commands-loadmap "docs/test.txt")
;;         [:set-position x y]
;;         [:move direction]))

;; (defn move-on-testmap
;;   [x y direction]
;;   (get-in (commands-to-state (commands-move-on-testmap x y direction))
;;           [:player :position]))

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

;; (defn start-fight
;;   []
;;   (with-redefs [rand (fn [] 0.98)]
;;     (commands-to-state (commands-move-on-testmap 1 1 :up))))

;; (defn start-fight-and-kill
;;   ([rolls]
;;    (let [[in out] (create-game-maser)
;;          a (start-fight-and-kill in out
;;                                  (run-game-logic [[:initialize]] false [] in out)
;;                                  rolls)]
;;      (a/close! in)
;;      (summarize-responses a)))
;;   ([in out a rolls]
;;    (let [r (atom (map dec rolls))]
;;      (with-redefs [rand (fn [] 0.98)
;;                    rand-int (fn [_]
;;                               (let [f (first @r)]
;;                                 (swap! r rest)
;;                                 f))]
;;        (run-game-logic (concat [[:move :up]]
;;                                [[:attack]])
;;                        false a in out)))))

;; (defn fight-until-game-over
;;   []
;;   (let [[in out] (create-game-maser)
;;         a (run-game-logic [[:initialize]] false [] in out)
;;         a (loop [i 0 a a]
;;             (if (< i 10)
;;               (recur (inc i) (start-fight-and-kill in out a [1 20 2 2 2 2 2 2 2]))
;;               a))]
;;     (a/close! in)
;;     (summarize-responses a)))

;; (deftest fight
;;   (testing "Starting a fight"
;;     (let [state (start-fight)]
;;       (is (some? (:fight state)))
;;       (is (clojure.string/starts-with? (first (:messages state))
;;                                        "You got attacked"))))
;;   (testing "attack and kill"
;;     (let [state (start-fight-and-kill [20 2 2 12 1 20 3 3 1 1 1 1 1 1 1])]
;;       (is (not (contains? state :fight)))
;;       (is (= 10 (get-in state [:player :hp 0])))
;;       (is (= 1 (get-in state [:player :xp])))
;;       (is (= "Beat: 4 :hp" (first (get-in state [:messages]))))))
;;   (testing "Fight until you die"
;;     (let [state (fight-until-game-over)]
;;       (is (= 0 (get-in state [:player :hp 0]))))))

;; (deftest Initalize-works
;;   (testing "I win a fight"
;;     (is (contains? (exec *game* [:initialize]) :board)))
;;   (testing "Bla"))
