(ns lum.picture-game
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [clojure.walk]
   [lum.maputil :as maputil]
   [ajax.core :as ajax]))


;; re-frame dispatcher

;;(rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
;;(rf/dispatch-sync [::rp/add-keyboard-event-listener "keypress"])
;;(rf/dispatch-sync [::rp/add-keyboard-event-listener "keyup"])

(def sizex 50)
(def sizey 30)

(def board-data
  (repeat (* sizex sizey) [:wall]))


(rf/reg-event-fx
 :game/initialize
 (fn [{:keys [db]} _]
   {:fx [[:dispatch [::rp/set-keypress-rules
                     {:event-keys [[[:game/key "left"] [{:keyCode 104}]];;h
                                   [[:game/key "right"] [{:keyCode 108}]];;l
                                   [[:game/key "up"] [{:keyCode 106}]];;k
                                   [[:game/key "down"] [{:keyCode 107}]];;j
                                   ]}]]
         [:dispatch [::rp/add-keyboard-event-listener "keypress"]]]

    :db (-> db
            (assoc :position {:x 0 :y 0})
            (assoc :board board-data))}))

(rf/reg-event-db
 :game/update-db
 (fn [db [_ response]]
   (assoc db :game/data
          {:images (:images response)
           :collumns (:collumns response)})))


(rf/reg-event-db
 :game/key
 (fn [db [_ direction]]
   (let [db (assoc-in db [:position :direction] direction)]
     (case direction
       "right" (update-in db [:position :x] inc)
       "left" (update-in db [:position :x] dec)
       "up" (update-in db [:position :y] inc)
       "down" (update-in db [:position :y] dec)
       db))))

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



(defn player [direction]
       (let [rotation (case direction
                        "left" 270
                        "up" 0
                        "right" 90
                        "down" 180
                        0)]
            [:img {:src "img/player.gif"
                   :style {
                           :transform (str "rotate(" rotation "deg)")
                           :width "15px"
                           :height "15px"
                           }}]))


(defn tile-data
  [board-data n]
  (nth board-data n :default))

(defn tile-to-graphic
  [key]
  (get {:wall "#"
        "wall" "8"
        "tree" "X"
        :default "."}
       key))


(defn board []
  (let [board (rf/subscribe [:game/board])
        position (rf/subscribe [:game/position])]
    (fn []
      (let [board @board
            [x y direction] @position]
        [:div.grid-container
         (for [i (range (* sizex sizey))]
           ^{:key (str "grid" i)}
           [:div.grid-item
            (if (= i (maputil/position-to-n x y))
              [player direction]
              (tile-to-graphic (get (tile-data board i)
                                    :type)))
            ])]))))

(defn new-map-button []
  [:input {:type "Button"
           :defaultValue "New map"
           :on-click (fn [] (rf/dispatch [:game/get-new-map]))}
])

(defn picture-game []
  [:section.section>div.container>div.content
  ;; [player]
   [board]
   [new-map-button]])
