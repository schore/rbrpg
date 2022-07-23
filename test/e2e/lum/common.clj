(ns lum.common
  (:require
   [etaoin.api :as e]
   [etaoin.keys :as keys]
   [user]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]))

(def ^:dynamic *driver*)

(defn fixture-start-server
  [f]
  (user/start)
  (f)
  (user/stop))

(defn fixture-driver
  "Intitalize web driver"
  [f]
  (e/with-firefox-headless {} driver
    (binding [*driver* driver]
      (f))))

(defn retry
  ([fn] (retry fn 20))
  ([fn n]
   (loop [n n]
     (if (or (fn)
             (= 0 n))
       nil
       (recur (dec n))))))

(defn delete-directory-recursive
  "Recursively delete a directory."
  [^java.io.File file]
  ;; when `file` is a directory, list its entries and call this
  ;; function with each entry. can't `recur` here as it's not a tail
  ;; position, sadly. could cause a stack overflow for many entries?
  ;; thanks to @nikolavojicic for the idea to use `run!` instead of
  ;; `doseq` :)
  (when (.isDirectory file)
    (run! delete-directory-recursive (.listFiles file)))
  ;; delete the file or directory. if it it's a file, it's easily
  ;; deletable. if it's a directory, we already have deleted all its
  ;; contents with the code above (remember?)
  (io/delete-file file))

(defn prepare-save-game
  [filename]
  (spit (str "tmp/" filename)
        (slurp (io/resource (str "savegames/" filename)))))

(defn fixture-prepare-directory
  [f]
  (.mkdir (io/file "tmp"))
  (prepare-save-game "got-two-batblood.edn")
  (prepare-save-game "one-hp-left-and-fighting.edn")
  (prepare-save-game "in-a-fight.edn")
  (prepare-save-game "on-stairs.edn")
  (prepare-save-game "test-map.edn")
  (f)
  (delete-directory-recursive (io/file "tmp")))

(defn open []
  (e/go *driver* "http://localhost:3000/"))

(defn open-website [f]
  (open)
  (f))

(defn refresh [f]
  (e/refresh *driver*)
  (f))

(defn click-menu-item
  [item]
  (e/click-visible *driver* [{:class :navbar-start}
                             {:tag :a :fn/text item}]))

(defn on-test-map
  []
  (e/click-visible *driver* {:tag :input :value "Load map"}))

(defn wait-map-screen
  []
  (e/wait-visible *driver* [{:class "grid-container"}
                            {:tag :img}]))

(defn map-screen?
  []
  (wait-map-screen)
  (e/exists? *driver* [{:class "grid-container"}
                       {:tag :img}]))

(defn fight-screen?
  []
  (e/exists? *driver* [{:class "content"}
                       {:tag :h1
                        :fn/has-text "FIGHT"}]))

(defn game-over?
  []
  (e/exists? *driver* [{:class "content"}
                       {:tag :h1
                        :fn/has-text "GAME OVER"}]))

(defn get-player-position
  []
  (let [query  [{:class "grid-container"}
                {:tag :img}]]
    (e/wait-exists *driver* query [:timeout 5])
    (->> (e/get-element-csss *driver* query  :left :top)
         (map (fn [inp] (apply str (filter #(Character/isDigit %) inp))))
         (map #(Integer/parseInt %))
         (map #(/ % 15)))))

(defn parse-item-str
  [str]
  (let [[_ k v] (re-matches #"(.*) ([\d+]*) [\d+]*" str)]
    [k  (Integer/parseInt v)]))

(defn get-items
  []
  (click-menu-item "Items")
  (let [query [{:class "items"}
               {:tag :table}
               {:tag :tr}]]
    (e/wait-visible *driver* query)
    (->> (e/query-all *driver* query)
         (map #(e/get-element-text-el *driver* %))
         (map parse-item-str)
         (into {}))))

(defn get-hp
  []
  (click-menu-item "Home")
  (Integer/parseInt (second (re-matches #"hp: (.*)/(.*)"
                                        (e/get-element-text *driver* [{:class "content"}
                                                                      {:tag :span
                                                                       :fn/has-text "hp:"}])))))

(defn get-item-row
  [item]
  (->> (e/query-all *driver*
                    [{:class "items"}
                     {:tag :table}
                     {:tag :tr}])
       (filter (fn [el]
                 (let [x (e/get-element-text-el *driver*
                                                (e/child *driver* el
                                                         {:tag :td
                                                          :index 1}))]
                   (= x item))))
       first))

(defn get-use-item-row
  [item]
  (->> (e/query-all *driver*
                    [{:class "items-use"}
                     {:tag :table}
                     {:tag :tr}])
       (filter (fn [el]
                 (let [x (e/get-element-text-el *driver* (e/child *driver* el
                                                                  {:tag :td
                                                                   :index 2}))]
                   (println x)
                   (= x
                      item))))
       first))

(defn get-plus-el
  [item]
  (e/child *driver* (get-item-row item)
           {:tag :input
            :value "+"}))

(defn use-item
  [item]
  (click-menu-item "Home")
  (println (get-use-item-row item))
  (e/click-el *driver*
              (e/child *driver* (get-use-item-row item)
                       {:tag :input
                        :value "use"})))
(defn select-items
  [item n]
  (let [el (get-plus-el item)]
    (dotimes [_ n]
      (e/click-el *driver* el))))

(defn combine
  [n item]
  (click-menu-item "Items")
  (select-items item n)
  (e/click *driver* {:tag :input
                     :type "button"
                     :value "combine"}))

(defn press-key
  [k]
  (e/perform-actions *driver*
                     (-> (e/make-key-input)
                         (e/add-key-press k))))

(defn move
  [direction]
  (press-key  (case direction
                :up "k"
                :down "j"
                :left "h"
                :right "l"))
  (e/wait *driver* 0.1))

(defn activate
  []
  (press-key keys/enter)
  (e/wait *driver* 0.4))

(defn load-game
  [filename]
  (click-menu-item "Home")
  (e/wait-visible *driver* [{:tag :input :type :text}])
  (e/clear *driver* [{:tag :input
                      :type :text}])
  (e/fill *driver* [{:tag :input
                     :type :text}]
          filename)
  (e/click *driver* [{:tag :input
                      :type :button
                      :value "load"}])
  (e/wait *driver* 1))

(defn equip
  [slot item]
  (click-menu-item "Home")
  (e/wait-visible *driver* [{:class "item-slots"}
                            {:tag :table}])
  (e/select *driver* (keyword slot) item))

(defn get-equipment-slot
  [slot]
  (click-menu-item "Home")
  (e/get-element-value *driver* (keyword slot)))
