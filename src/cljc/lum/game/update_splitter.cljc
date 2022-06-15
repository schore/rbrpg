(ns lum.game.update-splitter
  (:require [clojure.core.async :as a]))


(defn update-splitter
  [input-chan]
  (let [out (a/chan)]
    (a/go-loop [boards []]
      (let [data (a/<! input-chan)]
        (if (some? data)
          (do
            (when (not= boards (:boards data))
              (a/>! out [:boards (:boards data)]))
            (a/>! out [:data (dissoc data :boards)])
            (recur (:boards data)))
            (a/close! out))))
    out))
