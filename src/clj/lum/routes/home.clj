(ns lum.routes.home
  (:require
   [clojure.java.io :as io]
   [lum.game.cavegen :as cavegen]
   [lum.layout :as layout]
   [lum.middleware :as middleware]
   [ring.util.http-response :as response]
   [ring.util.response]
   [lum.game.load-save :as load-save]
   [clojure.edn :as edn]
   [clojure.spec.alpha :as s]
   [lum.game-config :as config]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  ["" {:middleware [;middleware/wrap-csrf
                    middleware/wrap-formats]}
   ["/" {:get home-page}]
   [(str "/" config/path "/docs.md") {:get (fn [_]
                                             (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                                                 (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   [(str "/" config/path "/maps/:map") {:get (fn [req]
                                               (let [id (get-in req [:path-params :map] "Noooo")]
                                                 (-> (response/ok (-> (str "docs/" id) io/resource slurp))
                                                     (response/header "Content-Type" "text/plain; charset=utf-8"))))}]
   [(str "/" config/path "/game/data/:id") {:get (fn [req]
                                                   (let [id (get-in req [:path-params :id])
                                                         savegame (load-save/load-rest-interface id)]
                                                     (if (some? savegame)
                                                       {:status 200
                                                        :headers {"Content-Type" "text/plain"}
                                                        :body (str savegame)}
                                                       {:status 404
                                                        :headers {"Content-Type" "text/plain"}
                                                        :body "save game not found"})))
                                            :put (fn [req]
                                                   (let [id (get-in req [:path-params :id])
                                                         data (-> (:body req)
                                                                  slurp
                                                                  edn/read-string)]
                                                     (if (s/valid? :game/game data)
                                                       (do
                                                         (load-save/save-game data [0 id])
                                                         {:status 200})
                                                       {:status 400
                                                        :headers {"Content-Type" "text/plain"}
                                                        :body "Input not conforming to spec\n"})))}]])
