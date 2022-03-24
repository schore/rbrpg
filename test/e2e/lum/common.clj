(ns lum.common
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

(defn map-screen?
  [driver]
  (e/exists? driver [{:class "grid-container"}
                     {:tag :img}]))

(defn wait-map-screen
  [driver]
  (e/wait-visible driver [{:class "grid-container"}
                          {:tag :img}]))

(defn fight-screen?
  [driver]
  (e/exists? driver [{:class "content"}
                     {:tag :h1
                      :fn/has-text "FIGHT"}]))

(defn game-over?
  [driver]
  (e/exists? driver [{:class "content"}
                     {:tag :h1
                      :fn/has-text "GAME OVER"}]))

(defn get-player-position
  [driver]
  (let [query  [{:class "grid-container"}
                {:tag :img}]]
    (e/wait-exists driver query [:timeout 5])
    (->> (e/get-element-csss driver query  :left :top)
         (map (fn [inp] (apply str (filter #(Character/isDigit %) inp))))
         (map #(Integer/parseInt %))
         (map #(/ % 15)))))

(defn parse-item-str
  [str]
  (let [[_ k v] (re-matches #"(.*) ([\d+]*) [\d+]*" str)]
    [k  (Integer/parseInt v)]))

(defn get-items
  [driver]
  (let [query [{:class "content"}
               {:tag :table}
               {:tag :tr}]]
    (->> (e/query-all driver query)
         (map #(e/get-element-text-el driver %))
         (map parse-item-str)
         (into {}))))

(defn get-plus-el
  [driver item]
  (->> (e/query-all driver
                    [{:class "content"}
                     {:tag :table}
                     {:tag :tr}])
       (filter (fn [el]
                 (let [x (e/get-element-text-el driver
                                                (e/child driver el
                                                         {:tag :td
                                                          :index 1}))]
                               (log/info x item )
                               (= x item))))
       (map (fn [el] (e/child driver el
                              {:tag :input
                               :value "+"})))
       first))

(defn combine
  [driver n item]
  (let [el (get-plus-el driver item)]
    (dotimes [_ n]
      (e/click-el driver el)))
  (e/click driver {:tag :input
                   :type "button"
                   :value "combine"}))

(defn press-key
  [driver k]
  (e/perform-actions driver
                     (-> (e/make-key-input)
                         (e/add-key-press k))))

(defn move
  [driver direction]
  (press-key driver (case direction
                      :up "k"
                      :down "j"
                      :left "h"
                      :right "l"))
  (e/wait driver 0.1))

(defn game-screen
  [f]
  (navigate-to-game *driver*)
  (f))
