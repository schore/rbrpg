(ns lum.game.chat
  (:require [clojure.spec.alpha :as s]
            [lum.game.game-database]))

(defn get-current-chat-message
  [data]
  (get-in data [:chat :communication (get-in data [:chat :chat-position])]))

(defn conform-statement
  [statement]
  (s/conform :lum.game.game-database/statement statement))

(defn continue [data _]
  (let [statement (conform-statement (get-current-chat-message data))]
    (println statement)
    (case (first statement)
      :message (update-in data [:chat :chat-position] inc)
      :jmp (dissoc data :chat)
      data)))
