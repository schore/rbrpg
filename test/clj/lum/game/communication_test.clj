(ns lum.game.communication-test
  (:require
   [clojure.string]
   [clojure.test :as t :refer [deftest is testing]]
   [lum.game-logic-dsl :as dsl]
   [lum.game.dataspec]))

(t/use-fixtures
  :each dsl/create-game)

(deftest in-chat
  (dsl/in-chat [["Bla"]])
  (is (dsl/in-chat?)))

(deftest action-gets-next-message
  (dsl/in-chat [["Bla"] ["Blub"]])
  (dsl/continue)
  (is (= ["Blub"] (dsl/get-active-chat))))

(deftest action-will-sent-a-message-event
  (dsl/in-chat [["Bla"] ["Blub"]])
  (dsl/continue)
  (is (= [:message "Blub"] (first (dsl/get-event)))))

(deftest leave-when-jump-on-exit
  (dsl/in-chat [["Bla" :exit]])
  (dsl/continue)
  (is (not (dsl/in-chat?))))

(deftest can-jump-around
  (dsl/in-chat [["Bla" :x] ["Foo"] [:x "Blub"]])
  (dsl/continue)
  (is (= [:x "Blub"] (dsl/get-active-chat))))

(deftest jumps-create-messages
  (dsl/in-chat [["Bla" :x] ["Foo"] [:x "Blub"]])
  (dsl/continue)
  (is (= [:message "Blub"] (first (dsl/get-event)))))

(deftest message-on-options
  (dsl/in-chat [["Bla"]
                [:option
                 "one" :one
                 "two" :two]])
  (dsl/continue)
  (is (= [:message ["one" :one
                    "two" :two]]
         (first (dsl/get-event)))))

(deftest jump-on-options
  (testing "First Option"
    (dsl/in-chat [[:option "one" :one "two" :two]
                  ["nop"]
                  [:one "one"]
                  [:two "two"]]))
  (dsl/continue :one)
  (is (= [:one "one"] (dsl/get-active-chat)))
  (testing "Second Option"
    (dsl/in-chat [[:option "one" :one "two" :two]
                  ["nop"]
                  [:one "one"]
                  [:two "two"]]))
  (dsl/continue :two)
  (is (= [:two "two"] (dsl/get-active-chat))))
