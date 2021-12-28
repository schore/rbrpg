(ns lum.game.update-data)

(defn fight
  [data new-data]
  (when (not= (:fight data)
              (:fight new-data))
    [:fight (:fight new-data)]))

(defn hp-update
  [data new-data]
  (when (not= (-> data :player :hp)
              (-> new-data :player :hp))
    (let [[current max] (-> new-data :player :hp)]
      [:hp current max])))

(defn mp-update
  [data new-data]
  (when (not= (-> data :player :mp)
              (-> new-data :player :mp))
    (let [[current max] (-> new-data :player :mp)]
      [:mp current max])))

(defn xp-update
  [data new-data]
  (when (not= (-> data :player :xp)
              (-> new-data :player :xp))
    [:xp (-> new-data :player :xp)]))

(defn board-update
  [data new-data]
  (if (not= (:board data)
            (:board new-data))
    [:new-board (:board new-data)]
    nil))

(defn player-move
  [data new-data]
  (when (not= (get-in data [:player :position])
              (get-in new-data [:player :position]))
    (let [[x y] (get-in new-data [:player :position])]
      [:player-move x y])))


(def update-calc-functions
  [board-update
   player-move
   fight
   hp-update
   mp-update
   xp-update])

(defn calc-updates [data new-data]
  (filter (complement nil?)
          (reduce (fn [r f]
                    ;;(log/info (f data new-data))
                    (conj r (f data new-data)))
                  []
                  update-calc-functions)))
