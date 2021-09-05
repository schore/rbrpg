(ns lum.integration-test
  (:require  [clojure.test :as t :refer [deftest
                                         testing
                                         is]]
             [etaoin.api :as e]))

(def ^:dynamic *driver*)

(defn fixture-driver
  "Intitalize web driver"
  [f]
  (e/with-firefox {} driver
    (binding [*driver* driver]
      (f))))

(defn open [driver]
  (doto driver
    (e/go "http://localhost:3000")))

(defn navigate-to-test [driver]
  (e/click-visible driver {:class "navbar-item" :href "#/test"}))

(defn get-count [driver]
  (e/get-element-text driver [{:class :content}
                              {:tag :p}]))

(defn click-count [driver]
  (e/click-visible driver [{:class :content}
                           {:tag :input}]))

(defn open-and-navigate [f]
  (doto *driver*
    (open)
    (navigate-to-test))
  (f))

(t/use-fixtures
  :each fixture-driver open-and-navigate)


(testing "Test application"
  (deftest inital-value
    (is (= "1" (get-count *driver*))))

  (deftest click-once
    (click-count *driver*)
    (is (= "2" (get-count *driver*))))

  (deftest click-three-times
    (click-count *driver*)
    (click-count *driver*)
    (click-count *driver*)
    (is (= "8" (get-count *driver*)))))
