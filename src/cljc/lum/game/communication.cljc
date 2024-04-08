(ns lum.game.communication
  (:require [clojure.spec.alpha :as s]
            [lum.game.utilities :as u]
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
      :jmp (-> cs second :msg))))

(defn create-message
  [data]
  (if (contains? data :chat)
    (update data :coeffects
            #(conj % [:message (get-message (get-current-statement data))]))
    data))

(defn find-jump-to
  ([data jumpto]
   (let [communication (-> data :chat :communication)
         n (first (keep-indexed (fn [index item]
                                  (when (= jumpto (first item)) index))
                                communication))]
     n)))

(defn handle-jmp
  ([data]
   (let [[_ jumpto] (get-current-statement data)]
     (handle-jmp data jumpto)))
  ([data jumpto]
   (if (= jumpto :exit)
     (dissoc data :chat)
     (assoc-in data [:chat :chat-position] (find-jump-to data jumpto)))))

(defn continue [data [_ jumpto]]
  (let [statement (conform-statement (get-current-statement data))]
    (-> (case (first statement)
          :message (update-in data [:chat :chat-position] inc)
          :jmp (-> data (handle-jmp))
          :option (-> data
                      (handle-jmp jumpto))
          :goto (update-in data [:chat :chat-position] inc))
        (create-message))))

(defn activate [data _]
  (if-let [chat (:npc (u/player-tile data))]
    (-> data
        (assoc-in [:chat :communication] chat)
        (assoc-in [:chat :chat-position] 0))
    data))
