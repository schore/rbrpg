(ns lum.routes.websockets
  (:require
   [org.httpkit.server :refer [send! as-channel]]
   [cheshire.core :as json]
   [clojure.walk]
   [lum.game.gamelogic :as gamelogic]
   [clojure.core.async :as a :refer [chan go go-loop <! >! close!]]
   [clojure.tools.logging :as log]
   [clojure.string]))


(defn connect!
  [in out]
  (fn [chan]
    (log/info "Channel open")
    (go (>! in [:initialize]))
    (go-loop []
      (when-let [msg (<! out)]
        (send! chan (json/generate-string msg))
        (recur)))))

(defn disconnect!
  [_ status]
  (log/info "Channel closed" status))

(defn handle-receive!
  [in]
  (fn
    [_ message]
    (log/info message)
    (let [message (json/parse-string-strict message)]
      (go (>! in message)))))

(defn ws-handler [request]
  (if-not (:websocket? request)
    {:status 200 :body "Game channel"}
    (let [in (chan)
          out (gamelogic/game-master in)]
      (as-channel request
                  {:on-open (connect! in out)
                   :on-close disconnect!
                   :on-receive (handle-receive! in)}))))

(def ws-routes
  ["/game/ws" ws-handler])
