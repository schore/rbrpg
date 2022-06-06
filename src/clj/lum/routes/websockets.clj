(ns lum.routes.websockets
  (:require
   [org.httpkit.server :refer [send! as-channel]]
   [cheshire.core :as json]
   [clojure.walk]
   [lum.game.gamelogic :as gamelogic]
   [lum.game.update-splitter :as update-splitter]
   [clojure.core.async :as a]
   [clojure.tools.logging :as log]
   [clojure.string]))


(defn create-game
  []
  (let [in (a/chan)
        out (update-splitter/update-splitter
             (gamelogic/game-master in))]
    [in out]))


(defn ws-handler [request]
  (if-not (:websocket? request)
    {:status 200 :body "Game channel"}
    (let [[in out] (create-game)]
      (as-channel request
                  {:on-open (fn [chan]
                              (log/info "Channel open")
                              (a/put! in [:initialize])
                              (a/go-loop []
                                (when-let [msg (a/<! out)]
                                  ;; (log/info msg)
                                  (send! chan (json/generate-string msg))
                                  (recur))))
                   :on-close (fn [_ status]
                               (log/info status)
                               (a/close! in))
                   :on-receive (fn [_ message]
                                 (log/info message)
                                 (a/put! in (json/parse-string message)))}))))

(def ws-routes
  ["/game/ws" ws-handler])
