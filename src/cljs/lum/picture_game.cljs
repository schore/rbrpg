(ns lum.picture-game
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [ajax.core :as ajax]))


;; re-frame dispatcher

;;(rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
;;(rf/dispatch-sync [::rp/add-keyboard-event-listener "keypress"])
;;(rf/dispatch-sync [::rp/add-keyboard-event-listener "keyup"])

(def size-x 50)
(def size-y 30)

(def board-data
  {[0 0] :wall
   [0 1] :wall
   [0 2] :wall
   [5 5] :tree
   [39 17] :wall})


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
   (let [
         db (assoc-in db [:position :direction] direction)]
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
   (println (str request))
   (let [update (into {} (map (fn [[k v]]
                                [k  (keyword v)])
                              request))]
     (assoc db :board update))))

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


(defn table-from-entries
  [n & entries]
  (let [entries (partition n n
                         (for [_ (range n)] "");;create a list in order for padding
                         entries)
        lines (count entries)]
     [:table>tbody
      (for [i (range lines)]
        ^{:key (str "table-from-entries-" i)}
        [:tr
         (for [j (range n)]
           ^{:key (str "table-from-entries-" i "-" j)}
           [:td (-> entries (nth i) (nth j))])])]))


(defn clickable-image
  ([src] (clickable-image src {}))
  ([src options]
   (let [options (merge {:active true
                         :max-width 200
                         :max-height 200
                         :on-click (fn [] nil)}
                        options)]
     [:div
      [:img {:src src
             :id (if (not (:active options)) "grayed" nil)
             :width (:max-width options)
             :height (:max-height options)
             :on-click (:on-click options)}]])))

(defn toggle-image [src]
  (let [active? (r/atom true)]
    (fn []
       [clickable-image src
        {:active @active?
         :on-click (fn []
                     (swap! active? not))}])))


(defn player []
   (let [position (rf/subscribe [:game/position])
         tile 15]
     (fn []
       (let [[x y direction] @position
             x (* x tile)
             y (* y tile)
             rotation (case direction
                        "left" 270
                        "up" 0
                        "right" 90
                        "down" 180
                        0)]
            [:img {:src "img/player.gif"
                   :style {:position "absolute"
                           :transform (str "rotate(" rotation "deg)")
                           :top y
                           :width "15px"
                           :height "15px"
                           :left x}}]))))

(defn position-to-n
  [x y]
    (+ x (* y size-x)))

(defn n-to-position
  [n]
  (let [x (mod n size-x)
        y (quot n size-x)]
    [x y]))


(defn tile-data
  [board-data n]
  (get board-data (n-to-position n) :default))

(defn tile-to-graphic
  [key]
  (get {:wall "#"
        :tree "X"
        :default "."}
       key))


(defn board []
  (let [board (rf/subscribe [:game/board])]
    (fn []
      (let [board @board]
        [:div.grid-container
         (for [i (range (* size-x size-y))]
           ^{:key (str "grid" i)}
           [:div.grid-item
            (tile-to-graphic (tile-data board i))
            ])]))))

(defn new-map-button []
  [:input {:type "Button"
           :defaultValue "New map"
           :on-click (fn [] (rf/dispatch [:game/get-new-map]))}
])

(defn picture-game []
  [:section.section>div.container>div.content
   [player]
   [board]
   [new-map-button]])
