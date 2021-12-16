(ns fight-test
  (:require [clojure.test :as t :refer [deftest is]]
            [e2e.common :as c :refer [*driver*]]
            [etaoin.api :as e]))

(t/use-fixtures :once c/fixture-driver c/open-website)

(t/use-fixtures :each c/refresh c/game-screen)

(defn enter-fight-screen
  [driver]
  (loop [i 0]
    (when (and (< i 1000)
               (not (e/visible? *driver* [{:class "content"}
                                          {:tag :h1
                                           :fn/has-text "FIGHT"}])))
      (c/move driver :left)
      (recur (inc i)))))

(deftest start-fight
  (enter-fight-screen *driver*)
  (is (e/visible? *driver* [{:class "content"}
                            {:tag :h1
                             :fn/has-text "FIGHT"}])))
