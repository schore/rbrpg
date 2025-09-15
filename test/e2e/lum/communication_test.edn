(ns lum.communication_test
  (:require
   [clojure.test :as t :refer [deftest is]]
   [etaoin.api :as e]
   [etaoin.keys :as keys]
   [lum.common :as c :refer [*driver*]]))

(t/use-fixtures :once c/fixture-start-server c/fixture-driver)

(t/use-fixtures :each c/fixture-prepare-directory c/open-website)

(deftest ^:integration enter-communication
  (c/load-game "chat.edn")
  (c/press-key keys/enter)
  (is (c/in-chat?)))
