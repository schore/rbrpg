(ns lum.picture-game
  (:require
    [reagent.core :as r]))



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

(defn picture-game []
  [:section.section>div.container>div.content
   [table-from-entries 3
    [toggle-image "img/0001.jpg"]
    [toggle-image "img/0002.jpg"]
    [toggle-image "img/0003.jpg"]
    [toggle-image "img/0004.jpg"]
    [toggle-image "img/0005.jpg"]
    [toggle-image "img/0006.jpg"]
    [toggle-image "img/0007.jpg"]]])
