(ns lum.game-views
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [lum.game-main]
            [lum.game.game-database :as db]
            [lum.maputil :as maputil]
            [lum.game.utilities :as u]
            [lum.game.communication :as communication]))

(def sizex maputil/sizex)
(def sizey maputil/sizey)

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
    (some? (:npc tile)) "@"
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

(defn communication-screen
  []
  (let [communication (rf/subscribe [:game/communication])]
    (fn []
      (let [communication @communication]
        [:<>
         [:h2 "Taliking"]
         [:p communication]]))))

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
      (let [items (filter (fn [[i _]] (u/useable-item? i)) @items)]
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
        game-over? (rf/subscribe [:game/game-over?])
        communication? (rf/subscribe [:game/communication?])]
    (fn []
      [:section.section>div.container>div.content
       [:div.outer-grid-container
        [:div.mainapplication
         (cond
           @game-over? [game-over]
           @fight? [fight-screen]
           @communication? [communication-screen]
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
