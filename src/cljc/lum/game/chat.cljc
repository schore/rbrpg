(ns lum.game.chat
  (:require [clojure.spec.alpha :as s]
            [lum.game.game-database]))

(defn get-current-statement
  [data]
  (get-in data [:chat :communication (get-in data [:chat :chat-position])]))

(defn conform-statement
  [statement]
  (s/conform :lum.game.game-database/statement statement))

(defn get-message
  [statement]
  (let [statement (conform-statement statement)]
    (case (first statement)
      :message (-> statement second :msg)
      :goto (-> statement second :msg))))

(defn create-message
  [data]
  (if (contains? data :chat)
    (update data :coeffects #(conj % [:message (get-message (get-current-statement data))]))
    data))

(defn handle-jmp
  [data]
  (let [[_ jumpto] (get-current-statement data)
        communication (-> data :chat :communication)
        n (first (keep-indexed (fn [index item]
                                 (when (= jumpto (first item)) index))
                               communication))]
    (if (= jumpto :exit)
      (dissoc data :chat)
      (assoc-in data [:chat :chat-position] n))))

(defn continue [data _]
  (let [statement (conform-statement (get-current-statement data))]
;;    (println statement)
    (case (first statement)
      :message (-> data
                   (update-in [:chat :chat-position] inc)
                   (create-message))
      :jmp (-> data
               (handle-jmp)
               (create-message))
      data)))
