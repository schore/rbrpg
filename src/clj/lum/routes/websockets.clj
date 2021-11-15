(ns lum.routes.websockets
  (:require
   [org.httpkit.server :refer [send! as-channel]]
   [cheshire.core :as json]
   [clojure.walk]
   [clojure.tools.logging :as log]))


(defn connect!
  [_]
  (log/info "Channel open"))

(defn disconnect!
  [_ status]
  (log/info "Channel closed" status))

(defmulti dispatch-ws
  (fn [_ msg]
    (keyword (:type msg))))

(defmethod dispatch-ws
  :player-move
  [_ msg]
  (log/info "Player move" (:direction msg)))

(defmethod dispatch-ws
  :default
  [_ msg]
  (log/info "default dispatch" msg))

(defn handle-receive!
  [channel message]
  (let [message (clojure.walk/keywordize-keys (json/parse-string message))]
    (log/info "notify-clients" message)
   (dispatch-ws channel message)))

(defn ws-handler [request]
  (if-not (:websocket? request)
    {:status 200 :body "Game channel"}
    (as-channel request
                {:on-open connect!
                 :on-close disconnect!
                 :on-receive handle-receive!})))

(def ws-routes
  ["/game/ws" ws-handler])
