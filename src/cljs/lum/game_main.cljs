(ns lum.game-main
  ;; (:require-macros
  ;;  [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [clojure.walk]
   [lum.maputil :as maputil]
   [lum.game.game-database :as gamedb]
   [lum.game.dataspec :as gamedata]
   [lum.game.gamelogic :as gl]
   [ajax.core :as ajax]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [clojure.edn :as edn]
   [clojure.walk]
   [cljs.core.async :as a :refer [<! >! go-loop go]]
   [lum.game.game-database :as db]
   [lum.game.update-splitter :as update-splitter]
   [lum.game.gamelogic :as gamelogic]
   [lum.game.fight :as fight]
   [lum.game.utilities :as u]
   [lum.game-config :as config]
   [lum.game.cavegen :as cavegen]))

(defn keyify-ws
  [msg]
  (let [msg (clojure.walk/keywordize-keys msg)]
    (print msg)
    (rf/dispatch
     (case (first msg)
       "data" [:game/update (second msg)]
       "boards" [:game/board-update (second msg)]))))

(defn create-ws
  []
  (let [stream  (ws/connect "ws://localhost:3000/game/ws"
                            {:format fmt/json})
        send-message (fn [msg] (go (>! (:sink (<! stream)) msg)))]

    (go (while (ws/connected? (<! stream))
          (let [message (<! (:source (<! stream)))]
            (keyify-ws message))))
    {:stream stream
     :send-message send-message}))

;; (defonce wsconn (create-ws))

(defn create-game
  []
  (let [in (a/chan)
        out (-> in
                gamelogic/game-master)]
    [in out]))

(defonce gamelogic (let [[in out] (create-game)]
                     {:in in
                      :out out}))

(go-loop []
  (when-let [msg (<! (:out gamelogic))]
    (rf/dispatch [:game/update msg])
    (recur)))

(rf/reg-fx
 :game/send-message
 (fn [msg]
   (print msg)
   (when (some? msg)
     (a/put! (:in gamelogic) msg))))
;; (rf/reg-fx
;;  :game/send-message
;;  (fn [msg] ((:send-message wsconn) msg)))

(def sizex 50)
(def sizey 30)

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
  (println min " " max " " entries)
  (let [n (+ max (- min) 1)
        sum (reduce + entries)]
    (+ (mod (- sum min) n) min)))

(rf/reg-event-fx
 :game/key
 (fn [{:keys [db]} [_ action]]
   (println action)
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
                          (println "ac" action " " ac)
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
   (->> (get-in db [:game :recepies] [])
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

(defn position-css [x y]
  {:width "15px"
   :height "15px"
   :position "absolute"
   :top (str (* y 15) "px")
   :left (str (* x 15) "px")})

(defn player []
  (let [player (rf/subscribe [:game/position])]
    (fn []
      (let [[x y] @player
            rotation 0]
        [:img {:src "img/player.gif"
               :style (assoc (position-css x y)
                             :transform (str "rotate(" rotation "deg)"))}]))))

(defn item-color
  [item]
  (let [rarity (get-in db/item-data [item :rarity] 20)]
    (cond
      (> rarity 9) :light-gray
      (> rarity 6) :gray
      (> rarity 4) :green
      (> rarity 2) :blue
      :else :gold)))

(defn tile-to-graphic
  [tile]
  (cond
    (not (:visible? tile)) "+"
    (seq (get tile :items {})) [:p {:style {:background-color
                                            (item-color (first (keys (get tile :items))))}}
                                "i"]
    (= :wall (:type tile)) "#"
    (= :ground (:type tile)) " "
    (= :stair-down (:type tile)) ">"
    (= :stair-up (:type tile)) "<"
    :else (str tile)))

(defn board []
  (let [board (rf/subscribe [:game/board])]
    (fn []
      (let [board @board]
        [:<>
         (for [i (range (* sizex sizey))]
           ^{:key (str "grid" i)}
           [:div.grid-item
            (tile-to-graphic (maputil/get-tile board i))])]))))

(defn button
  ([value event]
   [button value event nil])
  ([value event class]
   [:input {:type :button
            :class class
            :defaultValue value
            :on-click (fn [e]
                        (-> e .-target .blur)
                        (rf/dispatch event))}]))

(defn new-map-button []
  [:input {:type "Button"
           :defaultValue "New map"
           :on-click (fn [] (rf/dispatch [:game/get-new-map]))}])

(defn load-map-button []
  [:input {:type "Button"
           :defaultValue "Load map"
           :on-click (fn [e]
                       (-> e .-target .blur)
                       (rf/dispatch [:game/load-map]))}])

(defn stat-style
  ([[hp hp-max] [mp mp-max]]
   [:<>
    [:span {:style {:margin "10px"}} "hp: " hp "/" hp-max]
    [:span {:style {:margin "10px"}} "mp: " mp "/" mp-max]])
  ([xp hp mp]
   [:<>
    [:span {:style {:margin "10px"}} "xp: " xp]
    [stat-style hp mp]]))

(defn enemy-stat
  []
  (let [enemy (rf/subscribe [:game/enemy])]
    (fn []
      (let [enemy @enemy
            name (get enemy :name)]
        [:<> [:p name]
         [:p [stat-style (:hp enemy) (:mp enemy)]]]))))

(defn get-text
  [e]
  (if (coll? e)
    (first e)
    e))

(defn menu
  [entries active]
  [:<> (for [i (range 1 (count entries))]
         ^{:key (str "menu_" i)}
         [:p
          (when (= i active) {:style {:font-weight "bold"}})
          (let [entry (nth entries i)]
            (if (coll? entry)
              (first entry)
              entry))])])

(defn fight-menu
  [entry active]
  (let [items (get-in entry (butlast active))]
    [:<>
     ;; [:p (str entry)]
     ;; [:p (str active)]
     ;; [:p (str items)]
     [menu items (last active)]]))

(defn fight-screen
  []
  (let [actions (rf/subscribe [:game/action])]
    (fn []
      (let [{:keys [entries active]} @actions]
        [:<>
         [:h1 "FIGHT"]
         [:p>b "Enemy"]
         [enemy-stat]
         [fight-menu entries active]]))))

(defn game-over
  []
  [:h1 "GAME OVER"])

(defn stats
  []
  (let [hp (rf/subscribe [:player/hp])
        mp (rf/subscribe [:player/mp])
        xp (rf/subscribe [:player/xp])
        level (rf/subscribe [:game/level])]
    (fn []
      (let [hp @hp
            mp @mp
            xp @xp
            level @level]
        [:<>
         [stat-style xp hp mp]
         "level: " level]))))

(defn show-messages
  []
  (let [messages (rf/subscribe [:game/messages])]
    (fn []
      [:p
       (for [[i message] (map (fn [a b] [a b])
                              (range 10)
                              (take 10 @messages))]
         ^{:key (str "messsages_" i)}
         [:<> message
          [:br]])])))

(defn plus-minus-counter
  [selected-items k v]
  [:<>
   [:input {:type "button"
            :value "-"
            :on-click (fn [] (swap! selected-items #(update % k dec)))}]
   v
   [:input {:type "button"
            :value "+"
            :on-click (fn [] (swap! selected-items #(update % k inc)))}]])

(defn show-items
  []
  (let [items (rf/subscribe [:game/items])
        selected-items (r/atom {})]
    (fn []
      (when (not= (into #{} (map first @items))
                  (into #{} (map first @selected-items)))
        (reset! selected-items (into {} (for [[k _] @items] [k 0]))))
      (let [items @items
            sitems @selected-items]
        [:<>
         [:table.items>tbody (for [[k v] (sort items)]
                               ^{:key (str "show_item_" k)}
                               [:tr
                                [:td k]
                                [:td v]
                                [:td [plus-minus-counter selected-items k (get sitems k)]]])]
         [:input {:type "button"
                  :value "combine"
                  :on-click (fn [] (rf/dispatch [:game/combine sitems]))}]]))))

(defn recipies
  []
  (let [recipies (rf/subscribe [:game/recepies])]
    (fn []
      (let [recipies @recipies]
        [:<>
         [:h4 "Known recipies"]
         [:table.recepies>tbody
          (for [recipie recipies]
            [:tr
             [:td [button "Combine" [:game/combine (:incriedients recipie)]
                   (when (not (:possible? recipie)) :inactivebutton)]]
             [:td (:item recipie)]
             [:td (str (:incriedients recipie))]])]]))))

(defn items-for-use
  []
  (let [items (rf/subscribe [:game/items])]
    (fn []
      (let [items @items]
        [:table>tbody
         (for [[k v] (sort items)]
           ^{:key (str "show_items_use_" k)}
           [:tr
            [:td [button "use" [:game/use k]]]
            [:td k]
            [:td v]])]))))

(defn load-save
  []
  (let [filename (r/atom "xxxx")]
    (fn []
      (let [file @filename]
        [:div
         [:input {:type "text"
                  :value file
                  :on-change (fn [e] (reset! filename (-> e .-target .-value)))}]
         [button "load" [:game/load file]]
         [button "save" [:game/save file]]]))))

(defn item-slots
  []
  (let [equipment (rf/subscribe [:game/equipment])
        items (rf/subscribe [:game/items])]
    (fn []
      (let [equipment @equipment
            items @items]
        [:table>tbody
         (for [slot db/slots]
           ^{:key (str "item_slots_" slot)}
           [:tr
            [:td slot]
            [:td [:select
                  {:value (get equipment slot "none")
                   :id slot
                   :on-change (fn [e] (rf/dispatch [:game/equip slot (-> e .-target .-value)]))}
                  [:option "none"]
                  (for [item (filter #(>= (get items % 0) 1)
                                     (db/get-items-for-slot slot))]
                    ^{:key (str "item_slots_options_" item)}
                    [:option item])]]])]))))

(defn player-spells
  []
  (let [spells (rf/subscribe [:game/spells])
        fight? (rf/subscribe [:game/fight?])]
    (fn []
      (let [spells @spells
            fight? @fight?]
        [:table>tbody
         (when (not fight?)
           (for [spell (sort (filter #(= :player
                                         (get-in db/spells [% :target]))
                                     spells))]
             ^{:key (str "player_spells_" spell)}
             [:tr
              [:td [button "cast" [:game/cast spell]]]
              [:td spell]]))]))))

(defn game []
  (let [fight? (rf/subscribe [:game/fight?])
        game-over? (rf/subscribe [:game/game-over?])]
    (fn []
      [:section.section>div.container>div.content
       [:div.outer-grid-container
        [:div.mainapplication
         (cond
           @game-over? [game-over]
           @fight? [fight-screen]
           :else  [:div.grid-container
                   [board]
                   [player]])]

        [:div.stats [stats]]
        [:div.item-slots [item-slots]]
        [:div.messages [show-messages]]]
       [:div.outer-grid-container
        [:div.items-use [items-for-use]]
        [:div.spells [player-spells]]]
       [button "New game" [:game/new-game]]
       [button "New map" [:game/get-new-map]]
       [button "Load map" [:game/load-map]]
       [:br]
       [load-save]])))

(defn item []
  [:section.section>div.container>div.content
   [:div
    [recipies]
    [:hr]
    [show-items]]])
