(ns e2e.common
  (:require
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
    (e/go driver "http://localhost:3000/#/game"))

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

(defn refresh [f]
  (e/refresh *driver*)
  (f))

(defn navigate-to-game
  [driver]
  ;; (e/click-visible driver {:class "navbar-item" :href "#/game"})
  (e/click-visible driver {:tag :input :value "Load map"})
  ;; (e/wait-exists driver {:class "grid-container"})
                 )

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
  (e/perform-actions driver
   (-> (e/make-key-input)
       (e/add-key-press (case direction
                            :up "k"
                            :down "j"
                            :left "h"
                            :right "l")))))

(defn game-screen
  [f]
  (navigate-to-game *driver*)
  (f))
