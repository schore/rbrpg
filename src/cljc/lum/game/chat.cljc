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
  (let [cs (conform-statement statement)]
    (case (first cs)
      :message (-> cs second :msg)
      :goto (-> cs second :msg)
      :option (rest statement)
      "")))

(defn create-message
  [data]
  (if (contains? data :chat)
    (update data :coeffects
            #(conj % [:message (get-message (get-current-statement data))]))
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
    (-> (case (first statement)
          :message (update-in data [:chat :chat-position] inc)
          :jmp (-> data (handle-jmp))
          :option data
          data)
        (create-message))))
