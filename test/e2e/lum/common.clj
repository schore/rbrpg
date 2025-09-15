(ns lum.common
  (:require
   [etaoin.api :as e]
   [etaoin.keys :as keys]
   [user]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [lum.maputil :as mu]))

(def ^:dynamic *driver*)

(defn fixture-start-server
  [f]
  (user/start)
  (f)
  (user/stop))

(defn fixture-driver
  "Intitalize web driver"
  [f]
  (e/with-firefox {} driver
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
  (prepare-save-game "not-full-hp.edn")
  (prepare-save-game "items-two-combine.edn")
  (prepare-save-game "on-stairs-to-special-map.edn")
  (prepare-save-game "chat.edn")
  (f)
  (delete-directory-recursive (io/file "tmp")))

(defn open []
  (e/go *driver* "http://localhost:3000/"))

(defn open-website [f]
  (open)
  (f))

(defn click-menu-item
  [item]
  (e/click-visible *driver* [{:class :navbar-start}
                             {:tag :a :fn/text item}]))

(defn on-test-map
  []
  (e/click-visible *driver* {:tag :input :value "Load map"})
  (e/wait *driver* 0.5))

(defn new-map
  []
  (e/click-visible *driver* {:tag :input :value "New map"})
  (e/wait *driver* 0.5))

(defn wait-map-screen
  []
  (e/wait-visible *driver* [{:class "grid-container"}
                            {:tag :img}]))

(defn map-screen?
  []
  (e/wait *driver* 0.3)
  (e/exists? *driver* [{:class "grid-container"}
                       {:tag :img}]))

(defn fight-screen?
  []
  (e/exists? *driver* [{:class "content"}
                       {:tag :h1
                        :fn/has-text "FIGHT"}]))

(defn in-chat?
  []

  (loop [i 0]
    (let [result   (e/exists? *driver* [{:class "content"}
                                        {:tag :h2
                                         :fn/has-text "Talking"}])]
      (e/wait *driver* 0.1)
      (if (or result
              (> i 20))
        result
        (recur (inc i))))))

(defn game-over?
  []
  (e/exists? *driver* [{:class "content"}
                       {:tag :h1
                        :fn/has-text "GAME OVER"}]))

(defn get-player-position
  []
  (let [query  [{:class "grid-container"}
                {:tag :img}]]
    (e/wait-visible *driver* query [:timeout 10])
    (e/wait *driver* 0.5)
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
  (let [query [{:tag :table
                :class "items"}
               {:tag :tr}]]
    (e/wait-visible *driver* query)
    (->> (e/query-all *driver* query)
         (map #(e/get-element-text-el *driver* %))
         (map parse-item-str)
         (into {}))))

(defn parse-use-item-str
  [str]
  (let [[_ k v] (re-matches #"(.*) ([0-9]*)" str)]
    [k  (Integer/parseInt v)]))

(defn get-useable-items
  []
  (click-menu-item "Home")
  (let [query [{:class "items-use"}
               {:tag :table}
               {:tag :tr}]]
    (e/wait-visible *driver* query)
    (->> (e/query-all *driver* query)
         (map #(e/get-element-text-el *driver* %))
         (map parse-use-item-str)
         (into {}))))

(defn get-hp
  []
  (let [query [{:class "content"}
               {:tag :span
                :fn/has-text "hp:"}]]
    (click-menu-item "Home")
    (Integer/parseInt (second (re-matches #"hp: (.*)/(.*)"
                                          (do
                                            (e/wait-visible *driver* query)
                                            (e/get-element-text *driver* query)))))))

(defn get-item-row
  [item]
  (e/wait-visible *driver* [{:tag :table
                             :class "items"}])
  (->> (e/query-all *driver*
                    [{:tag :table
                      :class "items"}
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

(defn get-spell-item-row
  [spell]
  (->> (e/query-all *driver*
                    [{:class "spells"}
                     {:tag :table}
                     {:tag :tr}])
       (filter (fn [el]
                 (let [x (e/get-element-text-el *driver* (e/child *driver* el
                                                                  {:tag :td
                                                                   :index 2}))]
                   (= x spell))))
       first))

(defn cast-spell
  [spell]
  (click-menu-item "Home")
  (e/click-el *driver*
              (e/child *driver* (get-spell-item-row spell)
                       {:tag :input
                        :value "cast"})))

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
                :right "l"
                :down-left "b"
                :down-right "n"
                :up-left "y"
                :up-right "u"))
  (e/wait *driver* 0.2))

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
  (e/wait *driver* 0.5)
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

(defn get-recepies-table
  ([]
   (click-menu-item "Items")
   (let [query [{:tag :table
                 :class "recepies"}
                {:tag :tr}]]
     (e/wait-visible *driver* query)
     (->> (e/query-all *driver* query)
          (map (fn [el]
                 {:button (e/child *driver* el {:tag :input})
                  :item (e/get-element-text-el *driver*
                                               (e/child *driver* el {:tag :td :index 2}))
                  :recepie (e/get-element-text-el *driver*
                                                  (e/child *driver* el {:tag :td :index 3}))})))))
  ([item]
   (filter #(= item (:item %))) (get-recepies-table)))

(defn get-recepies
  []
  (map #(:item %) (get-recepies-table)))

(defn use-recipie
  [recipie]
  (let [t (->> (get-recepies-table)
               (filter #(= recipie (:item %)))
               first)]
    (e/click-el *driver* (:button t))))

(defn get-recipie-state
  [item]
  (let [attr (e/get-element-attr-el *driver*
                                    (:button (first (get-recepies-table item)))
                                    :class)]
    (println (str attr))
    (case attr
      "inactivebutton" :inactive
      :normal)))

(defn get-heading
  []
  (e/wait-visible *driver* {:tag :h1})
  (e/get-element-text *driver* {:tag :h1}))

(defn get-tile
  ([] (get-tile (get-player-position)))
  ([[x y]]
   (let [tile
         (e/get-element-text *driver* [{:class "grid-item"
                                        :index (inc (mu/position-to-n x y))}])]

     (case tile
       "<" :stair-down
       tile))))

(defn get-main-menu-entries
  []
  (map  #(e/get-element-text-el *driver* %)
        (e/query-all *driver* [{:class "navbar-menu"}
                               {:tag :a}])))
