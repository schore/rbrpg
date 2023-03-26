(ns lum.game-main
  ;; (:require-macros
  ;;  [cljs.core.async.macros :refer [go]])
  (:require
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [clojure.walk]
   [lum.maputil :as maputil]
   [ajax.core :as ajax]
   [clojure.edn :as edn]
   [cljs.core.async :as a :refer [<! go-loop]]
   [lum.game.game-database :as db]
   [lum.game.gamelogic :as gamelogic]
   [lum.game-config :as config]
   [lum.game.cavegen :as cavegen]))

(defn create-game
  []
  (let [in (a/chan)
        out (-> in
                gamelogic/game-logic)]
    [in out]))

(defonce gamelogic (let [[in out] (create-game)]
                     {:in in
                      :out out}))

(go-loop []
  (when-let [[event & data] (<! (:out gamelogic))]
    (case event
      :new-state (rf/dispatch [:game/update (first data)])
      :enter-unknown-level (rf/dispatch [:enter-unknown-level (first data)])
      (println event))

    (recur)))

(rf/reg-event-fx
 :enter-unknown-level
 (fn [_ [_ level]]
   (if (contains? db/special-maps level)
     {:http-xhrio {:method :get
                   :uri (str "/"  config/path "/maps/" (get-in db/special-maps [level :uri]))
                   :timeout 10000
                   :response-format (ajax/text-response-format)
                   :on-success [:game/map-resp level]}}
     {:game/send-message [:enter-unknown-level level (cavegen/get-dungeon)]})))

(rf/reg-event-fx
 :game/map-resp
 (fn [_ [_ level map]]
   (println level map)
   {:game/send-message [:enter-unknown-level level
                        (gamelogic/load-special-map map level)]}))

(rf/reg-fx
 :game/send-message
 (fn [msg]
   (when (some? msg)
     (a/put! (:in gamelogic) msg))))

(def sizex maputil/sizex)
(def sizey maputil/sizey)

(rf/reg-event-fx
 :game/initialize
 (fn [{:keys [db]} _]
   {:fx [[:dispatch [::rp/set-keypress-rules
                     {:event-keys [[[:game/key :left] [{:keyCode 104}]];;h
                                   [[:game/key :right] [{:keyCode 108}]];;l
                                   [[:game/key :down] [{:keyCode 106}]];;k
                                   [[:game/key :up] [{:keyCode 107}]];;j
                                   [[:game/key :confirm] [{:keyCode 13}]];;enter
                                   ]}]]
         [:dispatch [::rp/add-keyboard-event-listener "keypress"]]]
    :game/send-message [:initialize (cavegen/get-dungeon)]}))

(defn fight?
  [db]
  (some? (get-in db [:game :fight])))

(defn add-clockwise
  [min max & entries]
  (let [n (+ max (- min) 1)
        sum (reduce + entries)]
    (+ (mod (- sum min) n) min)))

(rf/reg-event-fx
 :game/key
 (fn [{:keys [db]} [_ action]]
   (merge
    (when (and (not (fight? db))
               (some #{action} [:up :down :left :right]))
      {:game/send-message [:move action]})
    (when (and (not (fight? db))
               (= action :confirm))
      {:game/send-message [:activate]})
    (when (and (fight? db)
               (some #{action} [:up :down]))
      (let [{:keys [entries active]} (:action db)
            n (dec (count (get-in entries (butlast active))))]
        {:db (update-in db [:action :active (dec (count active))]
                        #(add-clockwise 1 n % (if (= action :up) -1 1)))}))
    (when (and (fight? db)
               (= action :confirm))
      (let [{:keys [entries active]} (:action db)
            menu (first (get-in entries (butlast active)))
            action (get-in entries active)]
        {:game/send-message  (case menu
                               "Main" (case action
                                        "Attack" [:attack]
                                        "Run" [:flea]
                                        nil)
                               "Magic" [:cast-spell action]
                               nil)
         :db (update-in db [:action :active]
                        (fn [ac]
                          (if (coll? action)
                            (conj ac 1)
                            ac)))}))
    (when (and (fight? db)
               (= action :left))
      {:db (update-in db [:action :active]
                      #(if (= 1 (count %))
                         %
                         (into [] (drop-last %))))}))))

(rf/reg-event-fx
 :game/new-game
 (fn [_ _]
   {:game/send-message [:initialize (cavegen/get-dungeon)]}))

(rf/reg-event-fx
 :game/get-new-map
 (fn [_ _]
   {:game/send-message [:new-board (cavegen/get-dungeon)]}))

(rf/reg-event-fx
 :game/combine
 (fn [_ [_ items]]
   {:game/send-message [:combine items]}))

(rf/reg-event-fx
 :game/use
 (fn [_ [_ item]]
   {:game/send-message [:use-item item]}))

(rf/reg-event-fx
 :game/cast
 (fn [_ [_ spell]]
   {:game/send-message [:cast-spell spell]}))

(rf/reg-event-fx
 :game/load-map
 (fn [_ _]
   {:game/send-message [:load-map "docs/test.txt"]}))

(rf/reg-event-fx
 :game/load
 (fn [_ [_ fn]]
   {:http-xhrio {:method :get
                 :uri (str "/"  config/path "/game/data/" fn)
                 :timeout 10000
                 :response-format (ajax/text-response-format)
                 :on-success [:game/load-response]}}))

(rf/reg-event-fx
 :game/load-response
 (fn [_ [_ data]]
   {:game/send-message [:load (edn/read-string data)]}))

(rf/reg-event-fx
 :nop
 (fn [_ _]
   {}))

(rf/reg-event-fx
 :game/save
 (fn [{:keys [:db]} [_ fn]]
   (let [gamestate (-> (:game db))]
     {:http-xhrio {:method :put
                   :headers {"Content-Type" "text/plain"}
                   :uri (str "/" config/path "/game/data/" fn)
                   :response-format (ajax/text-response-format)
                   :on-success [:nop]
                   :body gamestate}})))

(rf/reg-event-fx
 :game/equip
 (fn [_ [_ slot item]]
   {:game/send-message (if (= "none" item)
                         [:unequip slot]
                         [:equip slot item])}))

(rf/reg-event-db
 :game/update
 (fn [db [_ game]]
   (let [fight-changed? (not= (contains? game :fight) (fight? db))
         db (assoc db :game game)
         fight-started? (and fight-changed? (fight? db))
         fight-ended? (and fight-changed? (not fight-started?))]
     (cond
       fight-started? (assoc db :action {:entries ["Main" "Attack"
                                                   (into [] (concat ["Magic"] (sort (get-in db [:game :player :spells]))))
                                                   "Run"]
                                         :active [1]})
       fight-ended? (dissoc db :action)
       :else db))))

(rf/reg-event-db
 :game/board-update
 (fn [db [_ boards]]
   (assoc db :boards boards)))

(rf/reg-sub
 :game/position
 (fn [db _]
   [(get-in db [:game :player :position 0])
    (get-in db [:game :player :position 1])]))

(rf/reg-sub
 :game/board
 (fn [db _]
   (get-in db [:game :boards (dec (get-in db [:game :level]))])))

(rf/reg-sub
 :game/fight?
 (fn [db _]
   (fight? db)))

(rf/reg-sub
 :game/action
 (fn [db _]
   (:action db)))

(rf/reg-sub
 :player/hp
 (fn [db _]
   (get-in db [:game :player :hp])))

(rf/reg-sub
 :player/mp
 (fn [db _]
   (get-in db [:game :player :mp])))

(rf/reg-sub
 :player/xp
 (fn [db _]
   (get-in db [:game :player :xp])))

(rf/reg-sub
 :game/game-over?
 (fn [db _]
   (= 0 (get-in db [:game :player :hp 0]))))

(rf/reg-sub
 :game/messages
 (fn [db _]
   (get-in db [:game :messages])))

(rf/reg-sub
 :game/enemy
 (fn [db _]
   (get-in db [:game :fight :enemy])))

(rf/reg-sub
 :game/items
 (fn [db _]
   (get-in db [:game :player :items])))

(defn enough-items?
  [data required-items]
  (every? (fn [[k v]] (<= v (get-in data [:player :items k] 0))) required-items))

(rf/reg-sub
 :game/recepies
 (fn [db _]
   (->> (get-in db [:game :player :recepies] [])
        (map (fn [incriedients]
               {:incriedients incriedients
                :item (get-in db/recipies [incriedients 0])
                :possible? (enough-items? (:game db) incriedients)})))))

(rf/reg-sub
 :game/equipment
 (fn [db _]
   (get-in db [:game :player :equipment])))

(rf/reg-sub
 :game/spells
 (fn [db _]
   (get-in db [:game :player :spells])))

(rf/reg-sub
 :game/level
 (fn [db _]
   (get-in db [:game :level])))
