(ns lum.fight-test
  (:require
   [clojure.test :as t :refer [deftest is]]
   [etaoin.api :as e]
   [etaoin.keys]
   [lum.common :as c :refer [*driver*]]
   [clojure.tools.logging :as log]))

(t/use-fixtures :once c/fixture-driver c/open-website)

(t/use-fixtures :each c/refresh c/game-screen)

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

(deftest start-fight
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

(deftest leave-fight
  (enter-fight-screen *driver*)
  (fight *driver*)
  (is (c/map-screen? *driver*))
  (is (seq (c/get-items *driver*))))


(deftest fight-until-end
  (loop [i 0]
    (when (and (not (c/game-over? *driver*))
               (< i 100))
      (enter-fight-screen *driver*)
      (fight *driver*)
      (recur (inc i))))
  (log/info (c/game-over? *driver*))
  (is (c/game-over? *driver*)))

(defn fight-until-you-get
  [n item]
 (loop [i 0]
    (when (and (not (c/game-over? *driver*))
               (< i 100)
               (< (get (c/get-items *driver*) item 0) n))
      (enter-fight-screen *driver*)
      (fight *driver*)
      (recur (inc i)))))


(deftest combine-item
  (fight-until-you-get 2 "batblood")
  (is (= 2 (get (c/get-items *driver*) "batblood")))
  (c/combine *driver* 2 "batblood")
  (is (= 1 (get (c/get-items *driver*) "healing potion"))))

(defn get-healing-potion
  []
  (fight-until-you-get 2 "batblood")
  (c/combine *driver* 2 "batblood"))


(deftest use-healing-potion
  (get-healing-potion)
  (let [hp (c/get-hp *driver*)]
    (c/use-item *driver* "healing potion")
    (is (< hp (c/get-hp *driver*)))))
