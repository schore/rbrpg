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

(deftest leave-fight
  (enter-fight-screen *driver*)
  (select-and-activate *driver* "Attack")
  (select-and-activate *driver* "Attack")
  (c/wait-map-screen *driver*))

(defn fight
  [driver]
  (enter-fight-screen driver)
  (when (c/fight-screen? driver) (select-and-activate driver "Attack"))
  (e/wait driver 0.1)
  (when (c/fight-screen? driver) (select-and-activate driver "Attack")))


(deftest fight-until-end
  (loop [i 0]
    (when (and (not (c/game-over? *driver*))
               (< i 100))
      (fight *driver*)
      (recur (inc i))))
  (log/info (c/game-over? *driver*))
  (is (c/game-over? *driver*)))
