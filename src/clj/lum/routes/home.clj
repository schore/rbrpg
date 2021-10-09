(ns lum.routes.home
  (:require
   [lum.layout :as layout]
   [clojure.java.io :as io]
   [lum.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn new-function []
  {:collumns 3
                                 :images ["img/0001.jpg"
                                          "img/0002.jpg"
                                          "img/0003.jpg"
                                          "img/0004.jpg"
                                          "img/0005.jpg"
                                          "img/0006.jpg"
                                          "img/0007.jpg"
                                          "img/0008.jpg"
                                          ]})

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
   ["/game/pics" {:get (fn [_]
                         {:status 200
                          :headers {"content-type" "application/json"}
                          :body (new-function)})}]])
