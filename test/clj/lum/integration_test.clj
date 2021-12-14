(ns lum.integration-test
  (:require  [clojure.test :as t :refer [deftest
                                         testing
                                         is]]
             [etaoin.api :as e]
             [clojure.tools.logging :as log]))

(def ^:dynamic *driver*)

(defn fixture-driver
  "Intitalize web driver"
  [f]
  (e/with-firefox {} driver
    (binding [*driver* driver]
      (f))))

(defn open [driver]
  (doto driver
    (e/go "http://localhost:3000")))

(defn navigate-to-test [driver]
  (e/click-visible driver {:class "navbar-item" :href "#/test"}))

(defn get-count [driver]
  (e/get-element-text driver [{:class :content}
                              {:tag :p}]))

(defn click-count [driver]
  (e/click-visible driver [{:class :content}
                           {:tag :input}]))

(defn open-website [f]
  (open *driver*)
  (f))

(defn navigate-to-game
  [driver]
  (e/click-visible driver {:class "navbar-item" :href "#/game"})
  (e/wait-exists driver {:class "grid-container"}))

;; (testing "Test application"
;;   (deftest inital-value
;;     (is (= "1" (get-count *driver*))))

;;   (deftest click-once
;;     (click-count *driver*)
;;     (is (= "2" (get-count *driver*))))

;;   (deftest click-three-times
;;     (click-count *driver*)
;;     (click-count *driver*)
;;     (click-count *driver*)
;;     (is (= "8" (get-count *driver*)))))

(defn get-player-position
  [driver]
  (let [query  [{:class "grid-container"}
                {:tag :img}]]
    (e/wait-exists driver query [:timeout 5])
    (->> (e/get-element-csss driver query  :left :top)
         (map (fn [inp] (apply str (filter #(Character/isDigit %) inp))))
         (map #(Integer/parseInt %))
         (map #(/ % 15)))))

(defn move
  [driver direction]
  (e/wait driver 1)
  (e/perform-actions *driver*
   (-> (e/make-key-input)
       (e/add-key-press (case direction
                            :up "k"
                            :down "j"
                            :left "h"
                            :right "l"))))

  (e/wait driver 1))

(defn game-screen
  [f]
  (navigate-to-game *driver*)
  (f))

(t/use-fixtures
  :each fixture-driver open-website game-screen)

(deftest game-load
    ;;(navigate-to-game *driver*)
  (is (= [10 10] (get-player-position *driver*))))

(deftest navigate-left
  (move *driver* :left)
  (is (= [9 10] (get-player-position *driver*))))

(deftest navigate-right
  (move *driver* :right)
  (is (= [11 10] (get-player-position *driver*))))

(deftest navigate-up
  (move *driver* :down)
  (is (= [10 11] (get-player-position *driver*))))

(deftest navigate-down
  (move *driver* :up)
  (is (= [10 9] (get-player-position *driver*))))
