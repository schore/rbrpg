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
  [driver]
  (loop [i 0]
    (when (and (< i 1000)
               (not (c/fight-screen? driver)))
      (c/move driver :left)
      (recur (inc i)))))

(defn select-and-activate
  [driver action]
  (is (c/fight-screen? driver))
  (let [bold "700"]
    (loop [i 0]
      (when (and (< i 20)
                 (not (= bold (e/get-element-css driver
                                                 [{:class "content"}
                                                  {:tag :p
                                                   :fn/has-text action}]
                                                 "font-weight"))))
        (c/press-key driver "j")
        (recur (inc i))))
    (is (= bold (e/get-element-css driver
                                   [{:class "content"}
                                    {:tag :p
                                     :fn/has-text action}]
                                   "font-weight")))
    (c/press-key driver etaoin.keys/space)))

(deftest ^:integration start-fight
  (enter-fight-screen *driver*)
  (is (e/visible? *driver* [{:class "content"}
                            {:tag :h1
                             :fn/has-text "FIGHT"}])))

(defn fight
  [driver]
  (loop [i 0]
    (when (and (< i 20)
               (c/fight-screen? driver))
        (select-and-activate driver "Attack")
        (e/wait driver 0.3)
        (recur (inc i)))))

(deftest ^:integration leave-fight
  (c/load-game *driver* "in-a-fight.edn")
  (fight *driver*)
  (is (c/map-screen? *driver*))
  (is (seq (c/get-items *driver*))))


(deftest ^:integration fight-until-end
  (c/load-game *driver* "one-hp-left-and-fighting.edn")
  (fight *driver*)
  (is (c/game-over? *driver*)))


(defn get-two-batblood
  []
  (c/load-game *driver* "got-two-batblood.edn")
  (is (= 2 (get (c/get-items *driver*) "batblood"))))

(deftest ^:integration combine-item
  (get-two-batblood)
  (c/combine *driver* 2 "batblood")
  (is (= 1 (get (c/get-items *driver*) "small healing potion"))))

(defn get-healing-potion
  []
  (get-two-batblood)
  (c/combine *driver* 2 "batblood"))

(defn got-damage
  []
  (is (not= 10 (c/get-hp *driver*)))
  (loop []
    (when (= 10 (c/get-hp *driver*))
        (enter-fight-screen *driver*)
        (fight *driver*)
        (recur))))

(deftest ^:integration use-healing-potion
  (get-healing-potion)
  (got-damage)
  (let [hp (c/get-hp *driver*)]
    (c/use-item *driver* "small healing potion")
    (is (< hp (c/get-hp *driver*)))))
