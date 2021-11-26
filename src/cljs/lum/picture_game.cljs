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


;; (go (let [stream (<! (ws/connect "ws://localhost:3000/game/ws" {:format fmt/json}))]
;;         (println "Hello world" stream)
;;         (>! (:sink stream) {:message "Bla"})
;;         (<! (a/timeout 1000))
;;         (println "Message send")
;;         (ws/close stream)))


(defmulti dispatch-ws
  (fn [msg]
    (println (str msg))
    (keyword (:type msg))))

(defmethod dispatch-ws
  :player-move
  [msg]
  (println "Player move" (str msg)))

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

;;(def bla (second (create-ws println)))
;; re-frame dispatcher


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
                                   ]}]]
         [:dispatch [::rp/add-keyboard-event-listener "keypress"]]]
    :db (-> db
            (assoc :position {:x 0 :y 0})
            (assoc :board board-data)
            (assoc :npc [{:x 10 :y 15}
                         {:x 10 :y 16}]))
    :game/send-message {:message "Hello World"
                        :type 3}}))


(defn player-move [board xp yp direction]
  (let [[x y] (case direction
                      :left [(dec xp) yp]
                      :right [(inc xp) yp]
                      :up [xp (dec yp)]
                      :down [xp (inc yp)]
                      [xp yp direction])]
    (if (= :wall (:type (maputil/get-tile board x y)))
      [xp yp]
      [x y])))

(rf/reg-event-fx
 :game/key
 (fn [{:keys [db]} [_ direction]]
   {:db (let [x (get-in db [:position :x])
              y (get-in db [:position :y])
              board (:board db)
              [x y] (player-move board x y direction)]
          (-> db
              (assoc-in [:position :x] x)
              (assoc-in [:position :y] y)
              (assoc-in [:position :direction] direction)))
    :game/send-message {:type :player-move
                        :direction direction}
    }))

(rf/reg-event-fx
 :game/get-new-map
 (fn [_ _]
   {:http-xhrio {:method :get
                 :uri "game/dungeon"
                 :response-format (ajax/json-response-format {:keywords? false})
                 :on-success [:game/set-new-map]
                 :on-failure [:game/error]}}))

(rf/reg-event-db
 :game/set-new-map
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
        "wall" "8"
        "tree" "X"
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
                                  :type))
            ])]))))

(defn new-map-button []
  [:input {:type "Button"
           :defaultValue "New map"
           :on-click (fn [] (rf/dispatch [:game/get-new-map]))}
])

(defn picture-game []
  [:section.section>div.container>div.content
   [:div.grid-container
    [board]
    [player]
    [monsters]]
   [new-map-button]])
