(ns lum.game.chat)

(defn continue [data p2]
  (println p2)
  (update-in data [:chat :chat-position] inc))
