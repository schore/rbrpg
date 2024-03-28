(ns lum.game.chat
  (:require [clojure.spec.alpha :as s]
            [lum.game.game-database]))

(defn get-current-chat-message
  [data]
  (get-in data [:chat :communication (get-in data [:chat :chat-position])]))

(defn conform-statement
  [statement]
  (s/conform :lum.game.game-database/statement statement))

(defn create-message
  [data]
  (update data :coeffects #(conj % [:message (first (get-current-chat-message data))])))

(defn handle-jmp
  [data]
  (let [[_ jumpto] (get-current-chat-message data)
        communication (-> data :chat :communication)
        n (.indexOf communication (first (filter #(= jumpto (first %)) communication)))]
    (if (= jumpto :exit)
      (dissoc data :chat)
      (assoc-in data [:chat :chat-position] n))))

(defn continue [data _]
  (let [statement (conform-statement (get-current-chat-message data))]
;;    (println statement)
    (case (first statement)
      :message (-> data
                   (update-in [:chat :chat-position] inc)
                   (create-message))
      :jmp (handle-jmp data)
      data)))
