(ns lum.routes.websockets
  (:require
   [org.httpkit.server :refer [send! as-channel websocket?]]
   [clojure.tools.logging :as log]))

;;(defonce instance (atom []))

(defn connect!
  [instance]
  (fn [channel]
    (log/info "Channel open")
    (swap! instance conj channel)))

(defn disconnect!
  [_]
  (fn [_ status]
    (log/info "Channel closed" status)))

(defn notify-clients!
  [instance]
  (fn [channel message]
    (log/info "notify-clients" channel message)
    (let [other-instance (remove #{channel} @instance)]
      (doseq [c other-instance]
        (send! c message)))))

(defn ws-handler [request]
  (if-not (:websocket? request)
    {:status 200 :body "Game channel"}
    (let [instance (atom [])]
      (as-channel request
                  {:on-open (connect! instance)
                   :on-close (disconnect! instance)
                   :on-receive (notify-clients! instance)}))))

(def ws-routes
  ["/game/ws" ws-handler])
