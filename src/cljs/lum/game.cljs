(ns lum.game
  ;; (:require-macros
  ;;  [cljs.core.async.macros :refer [go]])
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [clojure.walk]
   [lum.maputil :as maputil]
   [ajax.core :as ajax]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [clojure.walk]
   [cljs.core.async :as a :refer [<! >! go-loop go]]))

(defn keyify-ws
  [msg]
  (rf/dispatch [:game/update (clojure.walk/keywordize-keys msg)]))

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

(defonce wsconn (create-ws))

(rf/reg-fx
 :game/send-message
 (fn [msg] ((:send-message wsconn) msg)))

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
                                   [[:game/key :confirm] [{:keyCode 32}]];;space
                                   ]}]]
         [:dispatch [::rp/add-keyboard-event-listener "keypress"]]]}))

(defn fight?
  [db]
  (some? (get-in db [:game :fight])))

(rf/reg-event-fx
 :game/key
 (fn [{:keys [db]} [_ action]]
   (merge
    (when (and (not (fight? db))
               (some #{action} [:up :down :left :right]))
      {:game/send-message [:move action]})
    (when (and (fight? db)
               (some #{action} [:up :down]))
      (let [{:keys [entries active]} (:action db)
            n (count entries)]
        (println entries active n)
        {:db (assoc-in db [:action :active]
                       (mod (if (= action :down)
                              (inc active)
                              (dec active)) n))}))
    (when (and (fight? db)
               (= action :confirm))
      {:game/send-message (let [{:keys [entries active]} (:action db)
                                action (nth entries active)]
                            (case action
                              "Attack" [:attack]))}))))

(rf/reg-event-fx
 :game/get-new-map
 (fn [_ _]
   {:game/send-message [:new-board]}))

(rf/reg-event-fx
 :game/combine
 (fn [_ [_ items]]
   {:game/send-message [:combine items]}))

(rf/reg-event-fx
 :game/use
 (fn [_ [_ item]]
   {:game/send-message [:use-item item]}))

(rf/reg-event-fx
 :game/load-map
 (fn [_ _]
   {:game/send-message [:load-map "docs/test.txt"]}))

(rf/reg-event-fx
 :game/load
 (fn [_ [_ fn]]
   {:game/send-message [:load fn]}))

(rf/reg-event-fx
 :game/save
 (fn [_ [_ fn]]
   {:game/send-message [:save fn]}))


(rf/reg-event-db
 :game/update
 (fn [db [_ game]]
   (let [db (assoc db :game game)]
     (if (fight? db)
       (assoc db :action {:entries ["Attack" "Magic" "Run"]
                          :active 0})
       (dissoc db :action)))))

(rf/reg-sub
 :game/position
 (fn [db _]
   [(get-in db [:game :player :position 0])
    (get-in db [:game :player :position 1])]))

(rf/reg-sub
 :game/board
 (fn [db _]
   (get-in db [:game :board])))

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

(defn tile-to-graphic
  [key]
  (get {:wall "#"} key " "))

(defn board []
  (let [board (rf/subscribe [:game/board])]
    (fn []
      (let [board @board]
        [:<>
         (for [i (range (* sizex sizey))]
           ^{:key (str "grid" i)}
           [:div.grid-item
            (tile-to-graphic (keyword (get (maputil/get-tile board i) :type)))])]))))

(defn button
  [value event]
  [:input {:type :button
           :defaultValue value
           :on-click (fn [e]
                       (-> e .-target .blur)
                       (rf/dispatch event))}])

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

(defn fight-screen
  []
  (let [actions (rf/subscribe [:game/action])]
    (fn []
      (let [{:keys [entries active]} @actions]
        [:<>
         [:h1 "FIGHT"]
         [:p>b "Enemy"]
         [enemy-stat]
         (for [entry entries]
           ^{:key (str "fightscreen" entry)}
           [:p
            (when (= entry (nth entries active))
              {:style {:font-weight "bold"}})
            entry])]))))

(defn game-over
  []
  [:h1 "GAME OVER"])

(defn stats
  []
  (let [hp (rf/subscribe [:player/hp])
        mp (rf/subscribe [:player/mp])
        xp (rf/subscribe [:player/xp])]
    (fn []
      (let [hp @hp
            mp @mp
            xp @xp]
        [stat-style xp hp mp]))))

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
         [:table.tbody (for [[k v] items]
                         [:tr
                          [:td k]
                          [:td v]
                          [:td [:input {:type "button"
                                        :value "use"
                                        :on-click (fn [] (rf/dispatch [:game/use k]))}]]
                          [:td [plus-minus-counter selected-items k (get sitems k)]]])]
         [:input {:type "button"
                  :value "combine"
                  :on-click (fn [] (rf/dispatch [:game/combine sitems]))}]]))))

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

(defn game []
  (let [fight? (rf/subscribe [:game/fight?])
        game-over? (rf/subscribe [:game/game-over?])]
    (fn []
      [:section.section>div.container>div.content
       (cond
         @game-over? [game-over]
         @fight? [fight-screen]
         :else  [:div.grid-container
                 [board]
                 [player]])
       [button "New map" [:game/get-new-map]]
       [button "Load map" [:game/load-map]]
       [:br]
       [load-save]
       [stats]
       [show-items]
       [show-messages]])))