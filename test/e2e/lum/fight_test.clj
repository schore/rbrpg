(ns lum.fight-test
  (:require
   [clojure.test :as t :refer [deftest is]]
   [etaoin.api :as e]
   [etaoin.keys]
   [lum.common :as c :refer [*driver*]]
   [clojure.tools.logging :as log]))

(t/use-fixtures :once c/fixture-start-server c/fixture-driver c/open-website)

(t/use-fixtures :each c/fixture-prepare-directory c/refresh)

(defn enter-fight-screen
  []
  (loop [i 0]
    (when (and (< i 1000)
               (not (c/fight-screen?)))
      (c/move :left)
      (recur (inc i)))))

(defn select-and-activate
  [action]
  (is (c/fight-screen?))
  (let [bold "700"]
    (loop [i 0]
      (when (and (< i 20)
                 (not (= bold (e/get-element-css *driver*
                                                 [{:class "content"}
                                                  {:tag :p
                                                   :fn/has-text action}]
                                                 "font-weight"))))
        (c/press-key "j")
        (recur (inc i))))
    (is (= bold (e/get-element-css *driver*
                                   [{:class "content"}
                                    {:tag :p
                                     :fn/has-text action}]
                                   "font-weight")))
    (c/press-key etaoin.keys/enter)))

(defn fight
  []
  (is (c/fight-screen?) "Ensure you are really in a fight")
  (loop [i 0]
    (when (and (< i 20)
               (c/fight-screen?))
      (select-and-activate "Attack")
      (e/wait *driver* 0.3)
      (recur (inc i)))))

(defn get-two-batblood
  []
  (c/load-game "got-two-batblood.edn")
  (is (= 2 (get (c/get-items) "batblood"))))

(defn get-healing-potion
  []
  (get-two-batblood)
  (c/combine 2 "batblood"))

(defn got-damage
  []
  (is (not= 10 (c/get-hp)))
  (loop []
    (when (= 10 (c/get-hp))
      (enter-fight-screen)
      (fight)
      (recur))))

(deftest ^:integration use-healing-potion
  (get-healing-potion)
  (got-damage)
  (let [hp (c/get-hp)]
    (c/use-item "small healing potion")
    (is (< hp (c/get-hp)))))

(deftest ^:integration start-fight
  (enter-fight-screen)
  (is (e/visible? *driver* [{:class "content"}
                            {:tag :h1
                             :fn/has-text "FIGHT"}])))

(deftest ^:integration leave-fight
  (c/load-game "in-a-fight.edn")
  (fight)
  (is (c/map-screen?))
  (is (seq (c/get-items))))

(deftest ^:integration flea-from-fight
  ;;(c/load-game "in-a-fight.edn")
  (c/retry (fn []
             (c/load-game "in-a-fight.edn")
             (select-and-activate "Run")
             (c/map-screen?)))
  (is (c/map-screen?)))

(deftest ^:integration fight-until-end
  (c/load-game "one-hp-left-and-fighting.edn")
  (fight)
  (is (c/game-over?)))

(deftest ^:integration combine-item
  (get-two-batblood)
  (c/combine 2 "batblood")
  (is (= 1 (get (c/get-items) "small healing potion"))))

(deftest ^:integration recipie-remembered
  (c/load-game "items-two-combine.edn")
  (c/combine  2 "small healing potion")
  (is (some #{"medium healing potion"} (c/get-recepies))))

(deftest ^:integration use-recipie
  (c/load-game "items-two-combine.edn")
  (c/combine 2 "small healing potion");;learn a recipe
  (c/use-recipie "medium healing potion")
  (is (= 4 (get (c/get-items) "medium healing potion"))))

(deftest ^:integration use-magic
  (c/load-game "in-a-fight.edn")
  (select-and-activate "Magic")
  (select-and-activate "Burning Hands")
  (is (c/map-screen?)))

(deftest ^:integration use-magic-in-move-mode
  (c/load-game "not-full-hp.edn")
  (c/cast-spell "Healing")
  (is (= 7 (c/get-hp))))
