(ns lum.routes.websockets
  (:require
   [org.httpkit.server :refer [send! as-channel websocket?]]
   [clojure.tools.logging :as log]))

(defonce channels (atom []))

(defn connect!
  [channel]
  (log/info "Channel open")
  (swap! channels conj channel))

(defn disconnect! [channel status]
  (log/info "Channel closed" status)
  (swap! channels #(remove #{channel} %)))

(defn notify-clients! [channel message]
  (log/info "notify-clients" channel message)
  (let [other-channels (remove #{channel} @channels)]
    (doseq [c other-channels]
      (send! c message))))

(defn ws-handler [request]
  (if-not (:websocket? request)
    {:status 200 :body "This is a chatroom"}
    (as-channel request
                {:on-open connect!
                 :on-close disconnect!
                 :on-receive notify-clients!})))

(def ws-routes
  ["/ws" ws-handler])
