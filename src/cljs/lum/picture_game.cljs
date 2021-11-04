(ns lum.picture-game
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [clojure.walk]
   [lum.maputil :as maputil]
   [ajax.core :as ajax]))


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
                                   [[:game/key :up] [{:keyCode 106}]];;k
                                   [[:game/key :down] [{:keyCode 107}]];;j
                                   ]}]]
         [:dispatch [::rp/add-keyboard-event-listener "keypress"]]]

    :db (-> db
            (assoc :position {:x 0 :y 0})
            (assoc :board board-data)
            (assoc :npc [{:x 10 :y 15}
                         {:x 10 :y 16}]))}))

(rf/reg-event-db
 :game/update-db
 (fn [db [_ response]]
   (assoc db :game/data
          {:images (:images response)
           :collumns (:collumns response)})))

(defn player-move [board xp yp direction]
  (let [[x y] (case direction
                      :left [(dec xp) yp]
                      :right [(inc xp) yp]
                      :up [xp (inc yp)]
                      :down [xp (dec yp)]
                      [xp yp direction])]
    (if (= :wall (:type (maputil/get-tile board x y)))
      [xp yp]
      [x y]))
  )

(rf/reg-event-db
 :game/key
 (fn [db [_ direction]]
   (let [x (get-in db [:position :x])
         y (get-in db [:position :y])
         board (:board db)
         [x y] (player-move board x y direction)]
     (-> db
         (assoc-in [:position :x] x)
         (assoc-in [:position :y] y)
         (assoc-in [:position :direction] direction)))))

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
