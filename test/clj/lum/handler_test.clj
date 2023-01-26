(ns lum.handler-test
  (:require
   [clojure.test :refer [use-fixtures deftest testing is]]
   [lum.game-logic-dsl :as dsl]
   [lum.game.dataspec]
   [clojure.spec.alpha :as s]
   [clojure.data.json :as json]
   [ring.mock.request :refer [request body]]
   [lum.handler :refer [app]]
   [lum.middleware.formats :as formats]
   [muuntaja.core :as m]
   [mount.core :as mount]
   [clojure.edn :as edn]
   [ring.util.http-response :as response]
   [lum.game.load-save :as load-save]
   [lum.game-config :as config]))

(defn parse-json [body]
  (m/decode formats/instance "application/json" body))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'lum.config/env
                 #'lum.handler/app-routes)
    (f)))

(use-fixtures :each dsl/prepare-directory)

(deftest test-app
  (testing "main route"
    (let [response ((app) (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (request :get "/invalid"))]
      (is (= 404 (:status response))))))

(deftest gamestorage-load
  (dsl/prepare-save-game "load-test.edn")
  (let [response ((app) (request :get (str  "/" config/path "/game/data/load-test.edn")))]
    (is (= 200 (:status response)))
    (is (s/valid? :game/game
                  (edn/read-string  (:body response))))))

(deftest gamestorage-load-not-found
  (let [response ((app) (request :get (str "/" config/path  "/game/data/not-available.edn")))]
    (is (= 404 (:status response)))))

(deftest save-not-conformant-to-spec
  (is (= 400 (:status ((app)
                       (body (request :put (str "/" config/path "/game/data/foo.edn"))
                             "[1 2 3]"))))))

(deftest save-and-load
  (dsl/prepare-save-game "load-test.edn")
  (dsl/prepare-save-game "in-a-fight.edn")
  (let [valid-game (edn/read-string (slurp "tmp/load-test.edn"))
        response-save ((app) (-> (request :put (str "/" config/path "/game/data/in-a-fight.edn"))
                                 (body (str valid-game))))
        response-load ((app) (request :get (str "/" config/path "/game/data/in-a-fight.edn")))
        loaded-game (edn/read-string (:body response-load))]
    (is (= 200 (:status response-save)))
    (is (= 200 (:status response-load)))
    (is (= valid-game loaded-game))))
