(ns lum.game.update-splitter
  (:require [clojure.core.async :as a]
            [editscript.core :as e]))

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

(defn editscript-middleware
  [input-chan]
  (let [out (a/chan)]
    (a/go-loop [data {}]
      (let [new-data (a/<! input-chan)]
        (if (some? new-data)
          (do
            (a/>! out (e/diff data new-data))
            (recur new-data))
          (a/close! out))))
    out))
