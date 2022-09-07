(ns lum.game.view
  (:require [lum.game.utilities :as u]))

(defn process-view
  [data]
  (-> data
      (u/update-active-tile #(assoc % :visible? true))))
