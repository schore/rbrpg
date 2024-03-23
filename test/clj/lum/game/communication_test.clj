(ns lum.game.communication-test
  (:require
   [clojure.string]
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game-logic-dsl :as dsl]
   [lum.game.dataspec]
   [lum.maputil :as mu]))

(t/use-fixtures
  :each dsl/create-game)

(deftest in-chat
  (dsl/in-chat [["Bla"]])
  (is (dsl/in-chat?)))
