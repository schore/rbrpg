(ns lum.game.game-database-test
  (:require [lum.game.game-database :as sut]
            [clojure.test :as t :refer [deftest is]]
            [clojure.spec.alpha :as s]
            [clojure.core.async :as a]))

(deftest valid-enemy-database
  (is (s/valid? :lum.game.game-database/enemies sut/enemies)))

(deftest valid-recipie-database
  (is (s/valid? :lum.game.game-database/recipies sut/recipies)))

(deftest valid-item-db
  (is (s/valid? :lum.game.game-database/item-effects sut/item-data)))

(deftest valid-items-on-ground
  (is (s/valid? :lum.game.game-database/ground-items sut/items-on-ground)))

(deftest valid-spells
  (is (s/valid? :lum.game.game-database/spells sut/spells)))

(deftest valid-map-db
  (is (s/valid? :lum.game.game-database/map-db sut/special-maps)))

(s/def ::communication :lum.game.game-database/communication)

(deftest communication-valid-message
  (is (s/valid? ::communication [["Bla"]])))

(deftest communication-goto-valid
  (is (s/valid? ::communication [[:x "F"]])))

(deftest communication-exclude-keywords
  (doseq [keyword [:exit
                   :option
                   :action]]
    (t/testing keyword
      (is (not (s/valid? ::communication [[keyword "Bla"]]))))))

(deftest communication-valid-options
  (is (s/valid? ::communication [[:option "Bla" :x]
                                 [:x "F"]]))
  (is (not (s/valid? ::communication [[:option "Bla"]])))
  (is (not (s/valid? ::communication [[:option
                                       "A" :a "B" :b]
                                      [:a "a"]])))
  (is (s/valid? ::communication [[:option
                                  "A" :a "B" :b]
                                 [:a "a"]
                                 [:b "b"]])))

(deftest communication-jumps-valid
  (is (s/valid? ::communication [["F" :exit]]))
  (is (s/valid? ::communication [["F" :exit]
                                 [:bla "1"]]))
  (is (not (s/valid? ::communication [["F" :bla]])))

  (is (not (s/valid? ::communication [["F" :bla]
                                      ["B" :foo]
                                      [:bla "B"]])))
  (is (s/valid? ::communication [["F" :bla]
                                 ["A" :blub]
                                 [:bla "b"]
                                 [:blub "c"]])))

(deftest check-actions
  (doseq [action [:add-hp :add-mp]
          value [-8 0 2]]
    (t/testing action
      (is (s/valid? ::communication [[:action action value]])))))
