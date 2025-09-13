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

(deftest next-message-on-goto
  (dsl/in-chat [[:x "X"] ["Bla"]])
  (dsl/continue)
  (is (= ["Bla"] (dsl/get-active-chat))))

(deftest leave-when-jump-on-exit
  (dsl/in-chat [["Bla" :exit]])
  (dsl/continue)
  (is (not (dsl/in-chat?))))

(deftest leave-when-at-end
  (dsl/in-chat [["Hello"]])
  (dsl/continue)
  (is (not (dsl/in-chat?))))

(deftest can-jump-around
  (dsl/in-chat [["Bla" :x] ["Foo"] [:x "Blub"]])
  (dsl/continue)
  (is (= [:x "Blub"] (dsl/get-active-chat))))

(deftest message-for-goto
  (dsl/in-chat [["Bla" :x] ["Foo"] [:x "Blub"]])
  (dsl/continue)
  (is (= [:message "Blub"] (first (dsl/get-event)))))

(deftest message-for-jmp
  (dsl/in-chat [["Bla"] ["Blu" :a] [:a "a"]])
  (dsl/continue)
  (is (= [:message "Blu"] (first (dsl/get-event)))))

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

(deftest message-on-exit
  (doseq [chat [[["Hello"]]
                [["Hello" :exit]]
                [["Hello" :exit] ["Nope"]]]]
    (testing chat
      (dsl/in-chat chat)
      (dsl/continue)
      (is (= [:message :exit] (first (dsl/get-event)))))))

(deftest start-interaction
  (dsl/player-with-npc [["Hello"]])
  (dsl/activate)
  (is (dsl/in-chat?)))

(deftest start-interaction-creates-message
  (dsl/player-with-npc [["Hello"]])
  (dsl/activate)
  (is (= [:message "Hello"] (first (dsl/get-event)))))

(deftest start-no-interaction-without-npc
  (dsl/player-is-on :ground)
  (dsl/activate)
  (is (not (dsl/in-chat?))))
