(defproject rbrpg "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[ch.qos.logback/logback-classic "1.5.3"]
                 [cljs-ajax "0.8.4"]
                 [clojure.java-time "1.4.2"]
                 [com.cognitect/transit-clj "1.0.333"]
                 [com.cognitect/transit-cljs "0.8.280"]
                 [cprop "0.1.20"]
                 [day8.re-frame/http-fx "0.2.4"]
                 [expound "0.9.0"]
                 [funcool/struct "1.4.0"]
                 [com.google.javascript/closure-compiler-unshaded "v20240317"]
                 [json-html "0.4.7"]
                 [luminus-transit "0.1.6"]
                 ;; [luminus-undertow "0.1.11"]
                 [luminus-http-kit "0.2.0"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.12.1"]
                 [metosin/muuntaja "0.6.10"]
                 [metosin/reitit "0.6.0"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.18"]
                 [nrepl "1.1.1"]
                 [org.clojure/clojure "1.11.2"]
                 [org.clojure/clojurescript "1.11.132"]
                 [org.clojure/core.async "1.6.681"]
                 [org.clojure/tools.cli "1.1.230"]
                 [org.webjars.npm/bulma "1.0.0"]
                 [org.webjars.npm/material-icons "1.13.2"]
                 [org.webjars/webjars-locator "0.52"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [re-frame "1.4.3"]
                 [re-pressed "0.3.2"]
                 [reagent "1.2.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.12.1"]
                 [ring/ring-defaults "0.4.0"]
                 [selmer "1.12.59"]
                 [juji/editscript "0.6.3"]
                 [etaoin "1.0.40"]
                 [haslett "0.1.7"]
                 [thheller/shadow-cljs "2.28.3"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/e2e"]
  :test-selectors {:default (complement :integration)
                   :integration :integration}
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot lum.core

  :plugins [[lein-doo "0.1.11"]]
  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]

  :profiles {:uberjar {:omit-source true

                       :prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]
                       :aot :all
                       :uberjar-name "rbrpg.jar"
                       :source-paths ["env/prod/clj"  "env/prod/cljs"]
                       :resource-paths ["env/prod/resources"]}

             :dev           [:project/dev :profiles/dev]
             :test          [:project/dev :project/test :profiles/test]

             :project/dev  {:jvm-opts ["-Dconf=dev-config.edn" "-XX:-OmitStackTraceInFastThrow"]
                            :dependencies [[binaryage/devtools "1.0.7"]
                                           [cider/piggieback "0.5.3"]
                                           [pjstadig/humane-test-output "0.11.0"]
                                           [prone "2021-04-23"]
                                           [re-frisk "1.6.0"]
                                           [lambdaisland/kaocha "1.88.1376"]
                                           [lambdaisland/kaocha-junit-xml "1.17.101"]
                                           [lambdaisland/kaocha-cloverage "1.1.89"]
                                           [ring/ring-devel "1.12.1"]
                                           [ring/ring-mock "0.4.0"]]
                            :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                           [jonase/eastwood "0.3.5"]]
                            :source-paths ["env/dev/clj"  "env/dev/cljs" "test/cljs"]
                            :resource-paths ["env/dev/resources"]
                            :repl-options {:init-ns user
                                           :timeout 120000}
                            :injections [(require 'pjstadig.humane-test-output)
                                         (pjstadig.humane-test-output/activate!)]}
             :project/test {:prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "compile" "app"]]
                            :jvm-opts ["-Dconf=test-config.edn"]
                            :resource-paths ["env/test/resources"]}
             :kaocha {:prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "compile" "app"]]
                      :dependencies [[lambdaisland/kaocha "1.88.1376"]
                                     [lambdaisland/kaocha-junit-xml "1.17.101"]]}
             :profiles/dev {}
             :profiles/test {:plugins [[lein-test-report-junit-xml "0.2.0"]]}}
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]})
