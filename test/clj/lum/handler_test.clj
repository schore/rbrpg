(ns lum.handler-test
  (:require
    [clojure.test :refer [use-fixtures deftest testing is]]
    [clojure.data.json :as json]
    [ring.mock.request :refer [request]]
    [lum.handler :refer [app]]
    [lum.middleware.formats :as formats]
    [muuntaja.core :as m]
    [mount.core :as mount]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'lum.config/env
                 #'lum.handler/app-routes)
    (f)))

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "plus"
    (let [response ((app) (request :get "/plus/3/4"))
          body (json/read-str (slurp (:body response))
                              :key-fn keyword)]
      (is (= 200 (:status response)))
      (is (= 7 (:result body)))
      (is (= 3 (:x body)))
      (is (= 4 (:y body))))))
