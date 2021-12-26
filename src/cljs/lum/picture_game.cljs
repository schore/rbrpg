(ns lum.picture-game
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

;; websocket connection

(defmulti dispatch-ws
  (fn [msg]
    (keyword (first msg))))

(defmethod dispatch-ws
  :player-move
  [[_ x y]]
  (rf/dispatch [:game/set-player-postion x y]))

(defmethod dispatch-ws
  :new-board
  [[_ board]]
  (rf/dispatch [:game/set-board board]))

(defmethod dispatch-ws
  :fight
  [[_ fight?]]
  (rf/dispatch [:game/fight fight?]))


(defmethod dispatch-ws
  :xp
  [[_ xp]]
  (rf/dispatch [:player/xp xp]))

(defmethod dispatch-ws
  :hp
  [[_ current max]]
  (rf/dispatch [:player/hp current max]))

(defmethod dispatch-ws
  :mp
  [[_ current max]]
  (rf/dispatch [:player/mp current max]))

(defmethod dispatch-ws
  :default
  [msg]
  (println "Default handler" (str msg)))

(defn keyify-ws
  [msg]
  (dispatch-ws (clojure.walk/keywordize-keys msg)))

(defn create-ws
  [rx]
  (let [stream  (ws/connect "ws://localhost:3000/game/ws"
                            {:format fmt/json})
        send-message (fn [msg] (go (>! (:sink (<! stream)) msg)))]

    (go (while (ws/connected? (<! stream))
          (let [message (<! (:source (<! stream)))]
            (rx message))))
    {:stream stream
     :send-message send-message}))

(defonce wsconn (create-ws keyify-ws))

(rf/reg-fx
 :game/send-message
 (fn [msg] ((:send-message wsconn) msg)))

(def sizex 50)
(def sizey 30)

(def board-data
  (repeat (* sizex sizey) [:wall]))

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
         [:dispatch [::rp/add-keyboard-event-listener "keypress"]]]
    :db (-> db
            (assoc :position {:x 0 :y 0})
            (assoc :board board-data)
            (assoc :npc [{:x 10 :y 15}
                         {:x 10 :y 16}]))}))

(rf/reg-event-fx
 :game/key
 (fn [{:keys [db]} [_ action]]
   (merge
    (when (and (not (:fight? db))
               (some #{action} [:up :down :left :right] ))
      {:game/send-message [:move action]})
    (when (and (:fight? db)
               (some #{action} [:up :down]))
      (let [{:keys [entries active]} (:action db)
            n (count entries)]
        (println entries active n)
        {:db (assoc-in db [:action :active]
                       (mod (if (= action :down)
                              (inc active)
                              (dec active)) n))}))
    (when (and (:fight? db)
               (= action :confirm))
      {:game/send-message (let [{:keys [entries active]} (:action db)
                                action (nth entries active)]
                            (case action
                              "Attack" [:attack]))}))))


;; (rf/reg-event-fx
;;  :game/get-new-map
;;  (fn [_ _]
;;    {:http-xhrio {:method :get
;;                  :uri "game/dungeon"
;;                  :response-format (ajax/json-response-format {:keywords? false})
;;                  :on-success [:game/set-new-map]
;;                  :on-failure [:game/error]}}))

(rf/reg-event-db
 :game/set-player-postion
 (fn [db [_ x y]]
   (assoc-in db [:player :position] [x y])))

(rf/reg-event-db
 :game/fight
 (fn [db [_ fight?]]
   (merge db
          {:fight? fight?}
          (when fight? {:action {:entries ["Attack" "Magic" "Run"]
                                 :active 0}}))))

(rf/reg-event-fx
 :game/get-new-map
 (fn [_ _]
   {:game/send-message [:new-board]}))

(rf/reg-event-fx
 :game/load-map
 (fn [_ _]
   {:game/send-message [:load-map "docs/test.txt"]}))

(rf/reg-event-db
 :game/set-board
 (fn [db [_ request]]
   (assoc db :board (->> request
                         (map clojure.walk/keywordize-keys)
                         (map (fn [m] (update m :type keyword)))))))

(rf/reg-event-db
 :player/xp
 (fn [db [_ xp]]
   (assoc-in db [:player :xp] xp)))

(rf/reg-event-db
 :player/hp
 (fn [db [_ current max]]
   (assoc-in db [:player :hp] [current max])))

(rf/reg-event-db
 :player/mp
 (fn [db [_ current max]]
   (assoc-in db [:player :mp] [current max])))


(rf/reg-sub
 :game/collumns
 (fn [db _]
   (-> db :game/data :collumns)))

(rf/reg-sub
 :game/position
 (fn [db _]
   [(get-in db [:player :position 0])
    (get-in db [:player :position 1])]))

(rf/reg-sub
 :game/board
 (fn [db _]
   (:board db)))

(rf/reg-sub
 :game/npc
 (fn [db _]
   (:npc db)))

(rf/reg-sub
 :game/fight?
 (fn [db _]
   (:fight? db)))

(rf/reg-sub
 :game/action
 (fn [db _]
   (:action db)))

(rf/reg-sub
 :player/hp
 (fn [db _]
   (-> db :player :hp)))

(rf/reg-sub
 :player/mp
 (fn [db _]
   (-> db :player :mp)))


(rf/reg-sub
 :player/xp
 (fn [db _]
   (-> db :player :xp)))


(rf/reg-sub
 :game/game-over?
 (fn [db _]
   (= 0 (get-in db [:player :hp 0]))))


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
  (get {:wall "#"
        :default "."}
       key))

(defn board []
  (let [board (rf/subscribe [:game/board])]
    (fn []
      (let [board @board]
        [:<>
         (for [i (range (* sizex sizey))]
           ^{:key (str "grid" i)}
           [:div.grid-item
            (tile-to-graphic (get (maputil/get-tile board i)
                                  :type))])]))))

(defn button
  [value event]
  [:input {:type "Button"
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

(defn fight-screen
  []
  (let [actions (rf/subscribe [:game/action])]
    (fn []
      (let [{:keys [entries active]} @actions]
        [:<>
         [:h1 "FIGHT"]
         (for [entry entries]
           ^{:key (str"fightscreen" entry)}
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
      (let [[hp hp-max] @hp
            [mp mp-max] @mp
            xp @xp]
        [:<>
         [:span {:style {:margin "10px"}} "xp: " xp]
         [:span {:style {:margin "10px"}} "hp: " hp "/" hp-max]
         [:span {:style {:margin "10px"}} "mp: " mp "/" mp-max]]))))


(defn picture-game []
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
       [stats]])))
