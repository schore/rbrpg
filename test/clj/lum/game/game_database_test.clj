(ns lum.game.game-database-test
  (:require [lum.game.game-database :as sut]
            [clojure.test :as t]
            [clojure.spec.alpha :as s]))


(t/deftest valid-enemy-database
  (t/is (s/valid? :lum.game.game-database/enemies sut/enemies)))

(t/deftest valid-recipie-database
  (t/is (s/valid? :lum.game.game-database/recipies sut/recipies)))

(t/deftest valid-item-db
  (t/is (s/valid? :lum.game.game-database/item-effects sut/item-effects)))

(t/deftest valid-items-on-ground
  (t/is (s/valid? :lum.game.game-database/ground-items sut/items-on-ground)))
