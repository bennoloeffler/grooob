(defproject re-pipe "0.1.0-SNAPSHOT"

  :description ""
  :url ""

  :dependencies [[ch.qos.logback/logback-classic "1.2.10"]
                 [cljs-ajax "0.8.4"]
                 ;[clojure.java-time "0.3.3"]
                 [com.cognitect/transit-clj "1.0.329"]
                 [com.cognitect/transit-cljs "0.8.269"]
                 [cprop "0.1.19"]
                 [day8.re-frame/http-fx "0.2.4"]
                 [expound "0.9.0"]
                 [funcool/struct "1.4.0"]
                 [json-html "0.4.7"]
                 ;[luminus-transit "0.1.3"]
                 [luminus-undertow "0.1.14"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.8"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.15"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.16"]
                 [nrepl "0.9.0"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.11.4" :scope "provided"]
                 [org.clojure/core.async "1.5.648"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.webjars.npm/bulma "0.9.3"]
                 [org.webjars.npm/material-icons "1.0.0"]
                 [org.webjars/webjars-locator "0.42"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [re-frame "1.2.0"]
                 [reagent "1.1.0"]
                 [re-pressed "0.3.2"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.5"]
                 [ring/ring-defaults "0.3.3"]
                 [selmer "1.12.50"]
                 [thheller/shadow-cljs "2.17.0" :scope "provided"]
                 [hiccup "1.0.5"]

                 [io.replikativ/konserve "0.7.294"]
                 [io.replikativ/datahike "0.6.1531"]
                 [io.replikativ/datahike-jdbc "0.1.2-SNAPSHOT"]
                 [com.hyperfiddle/rcf "20220405"]
                 [buddy "2.0.0"]
                 [ring-oauth2 "0.2.0"]
                 [clj-http "3.12.3"]

                 [belib "0.1.0-SNAPSHOT"]
                 [clojure.java-time "1.2.0"]
                 [io.github.erdos/erdos.assert "0.2.3"]

                 [metosin/malli "0.11.0"]

                 [tick "0.6.2"]
                 [net.cgrand/macrovich "0.2.1"]
                 [luminus-transit "0.1.6"]

                 [com.github.gnl/playback "0.3.10"]
                 [lambdaisland/deep-diff2 "2.10.211"]

                 [mvxcvi/puget "1.3.2"] ;colour print data
                 [instaparse "1.4.12"]]



  :min-lein-version "2.0.0"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj" "test/cljc" "src/clj" "src/cljc" "checkouts/belib/src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot re-pipe.core

  :plugins []
  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]

  :aliases {"kaocha"   ["with-profile" "+kaocha" "run" "-m" "kaocha.runner" #_"--plugin" #_"notifier" "--watch"]
            "coverage" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner" "--plugin" "cloverage"]}

  :profiles
  {:uberjar       {:omit-source    true

                   :prep-tasks     ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]
                   :aot            :all
                   :uberjar-name   "re-pipe.jar"
                   :source-paths   ["env/prod/clj" "env/prod/cljs"]
                   :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :kaocha        {:dependencies [[lambdaisland/kaocha "1.67.1055"]
                                  [lambdaisland/kaocha-cloverage "1.0.75"]]}


   :project/dev   {:jvm-opts       ["-Dconf=dev-config.edn"
                                    "-Djdk.attach.allowAttachSelf"]
                   :dependencies   [[binaryage/devtools "1.0.4"]
                                    [cider/piggieback "0.5.3"]
                                    [org.clojure/tools.namespace "1.2.0"]
                                    [pjstadig/humane-test-output "0.11.0"]
                                    [prone "2021-04-23"]
                                    [com.clojure-goes-fast/clj-async-profiler "1.0.3"]

                                    [re-frisk "1.5.2"]
                                    [ring/ring-devel "1.9.5"]
                                    [ring/ring-mock "0.4.0"]]
                   ; included in ~/.lein/projects.clj
                   ;[philoskim/debux "0.8.3"]
                   ;[djblue/portal "0.37.1"]
                   ;[com.github.gnl/playback "0.3.10"]]

                   :plugins        [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                    [jonase/eastwood "0.3.5"]
                                    [cider/cider-nrepl "0.26.0"]]


                   :source-paths   ["checkouts/belib/src/cljc" "env/dev/clj" "env/dev/cljs" "test/cljs"]
                   :resource-paths ["env/dev/resources"]
                   :repl-options   {:init-ns user
                                    :timeout 120000}
                   :injections     [(require 'pjstadig.humane-test-output)
                                    (pjstadig.humane-test-output/activate!)]}
   :project/test  {:jvm-opts       ["-Dconf=test-config.edn"]
                   :resource-paths ["env/test/resources"]}



   :profiles/dev  {}
   :profiles/test {}})
