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
   [clojure.spec.alpha :as s]))

(defn home-page [request]
  (layout/render request "home.html"))



(defn home-routes []
  [""
   {:middleware [
                 ;middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/plus/:x/:y" {:get (fn [req]
                          (let [x (read-string (-> req :path-params :x))
                                y (read-string (-> req :path-params :y))]
                            {:status 200
                             :headers {"content-type" "application/json"}
                             :body {:x x
                                    :y y
                                    :result (+ x y)}}))}]
   ["/game/data/:id" {:get (fn [req]
                             (let [id (get-in req [:path-params :id])
                                   savegame (load-save/load-rest-interface id)]
                               (if (some? savegame)
                                 {:status 200
                                  :header {:content-type "text"}
                                  :body (str savegame)}
                                 {:status 404
                                  :body "save game not found"})))
                      :put (fn [req]
                             (println "PUT2\n" (:body req))
                             (let [id (get-in req [:path-params :id])
                                   data (-> (:body req)
                                            slurp
                                            edn/read-string)]
                               (if (s/valid? :game/game data)
                                 (do
                                   (load-save/save-game data [0 id])
                                   {:status 200})
                                 {:status 400
                                  :body "Input not conforming to spec\n"
                                  })))}]
   ["/game/dungeon" {:get (fn [_]
                            {:status 200
                             :headers {"content-type" "application/json"}
                             :body (into [] (cavegen/get-dungeon))
                             })}]])
