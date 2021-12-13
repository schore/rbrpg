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
                              (dec active)) n))})))))


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
   (-> db
       (assoc-in [:position :x] x)
       (assoc-in [:position :y] y))))

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

(rf/reg-sub
 :game/collumns
 (fn [db _]
   (-> db :game/data :collumns)))

(rf/reg-sub
 :game/position
 (fn [db _]
   [(-> db :position :x)
    (-> db :position :y)
    (-> db :position :direction)]))

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

(defn position-css [x y]
  {:width "15px"
   :height "15px"
   :position "absolute"
   :top (str (* y 15) "px")
   :left (str (* x 15) "px")})

(defn player []
  (let [player (rf/subscribe [:game/position])]
    (fn []
      (let [[x y direction] @player
            rotation (case direction
                       :left 270
                       :up 0
                       :right 90
                       :down 180
                       0)]
        [:img {:src "img/player.gif"
               :style (assoc (position-css x y)
                             :transform (str "rotate(" rotation "deg)"))}]))))

(defn tile-to-graphic
  [key]
  (get {:wall "#"
        :default "."}
       key))

(defn monsters []
  (let [mon (rf/subscribe [:game/npc])]
    [:<>
     (for [m @mon]
       ^{:key (str "monster" m)}
       [:div {:style (position-css (:x m) (:y m))}
        "M"])]))

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

(defn new-map-button []
  [:input {:type "Button"
           :defaultValue "New map"
           :on-click (fn [] (rf/dispatch [:game/get-new-map]))}])

(defn load-map-button []
  [:input {:type "Button"
           :defaultValue "Load map"
           :on-click (fn [] (rf/dispatch [:game/load-map]))}])

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

(defn picture-game []
  (let [fight? (rf/subscribe [:game/fight?])]
    (fn []
      [:section.section>div.container>div.content
       (if @fight?
         [fight-screen]
         [:div.grid-container
          [board]
          [player]])
       [new-map-button]
       [load-map-button]])))
