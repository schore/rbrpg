(ns lum.routes.home
  (:require
   [lum.layout :as layout]
   [clojure.java.io :as io]
   [lum.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [lum.routes.game.cavegen :as cavegen]))

(defn home-page [request]
  (layout/render request "home.html"))



(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
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
   ["/game/dungeon" {:get (fn [_]
                            {:status 200
                             :headers {"content-type" "application/json"}
                             :body (into [] (cavegen/get-dungeon))
                             })}]])
