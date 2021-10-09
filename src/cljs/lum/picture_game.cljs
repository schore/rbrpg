(ns lum.picture-game
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [ajax.core :as ajax]))


;; re-frame dispatcher

;;(rf/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
(rf/dispatch-sync [::rp/add-keyboard-event-listener "keypress"])
;;(rf/dispatch-sync [::rp/add-keyboard-event-listener "keyup"])

(rf/reg-event-fx
 :game/initialize
 (fn [{:keys [db]} _]
   ;; {:http-xhrio {:method :get
   ;;               :uri "game/pics"
   ;;               :response-format (ajax/json-response-format {:keywords? true})
   ;;               :on-success [:game/update-db]
   ;;               :on-failure [:game/error]}
   {:dispatch [::rp/set-keypress-rules
               {:event-keys [[[:game/key "left"] [{:keyCode 104}]];;h
                             [[:game/key "right"] [{:keyCode 108}]];;l
                             [[:game/key "up"] [{:keyCode 107}]];;k
                             [[:game/key "down"] [{:keyCode 106}]];;j
                             ]}]
    :db (assoc db :position {:x 0 :y 0})}))

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


(rf/reg-sub
 :game/pics
 (fn [db _]
   (-> db :game/data :images)))

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
         tile 32]
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
                           :bottom y
                           :left x}}]))))

(defn position-to-n
  [x y]
  (let [size-x 40
        size-y 17]
    (+ x (- (* size-x size-y) (* size-x y)))))

(defn n-to-position
  [n]
  (let [size-x 40
        size-y 17
        x (mod n size-x)
        y (- size-y (/ (- n x) size-x))]
    [x y]))

(def board-data
  {[0 0] :wall
   [0 1] :wall
   [5 5] :tree
   [39 17] :wall})

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
  [:div.grid-container
   (for [i (range 720)]
     ^{:key (str "grid" i)}
     [:div.grid-item
      (tile-to-graphic (tile-data board-data i))
      ])])

(defn picture-game []
  [:section.section>div.container>div.content
   [board]
   [player]])
