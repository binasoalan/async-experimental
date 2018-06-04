(defproject binasoalan "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [io.pedestal/pedestal.jetty "0.5.3"]
                 [io.pedestal/pedestal.service "0.5.3"]
                 [org.postgresql/postgresql "42.2.2"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [com.layerware/hugsql "0.4.8"]
                 [hikari-cp "2.4.0"]
                 [migratus "1.0.6"]
                 [enlive "1.1.6"]
                 [hiccup "1.0.5"]
                 [buddy "2.0.0"]
                 [com.draines/postal "2.0.2"]
                 [environ "1.1.0"]
                 [funcool/struct "1.2.0"]
                 [ring/ring-core "1.6.3"]
                 [metosin/ring-http-response "0.9.0"]
                 [org.clojure/core.async "0.4.474"]
                 [cheshire "5.8.0"]

                 [org.clojure/clojurescript "1.10.238"]
                 [cljs-ajax "0.7.3"]
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]
                 [secretary "1.2.3"]
                 [day8.re-frame/http-fx "0.1.6"]

                 [ch.qos.logback/logback-classic "1.2.3"
                  :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-environ "1.1.0"]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["config" "resources" "target/resources"]
  :test-paths ["test/clj"]
  :target-path "target/%s"
  :main ^:skip-aot binasoalan.server

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :auto-clean false

  :figwheel {:css-dirs ["resources/public/css"]}

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src/cljs" "src/cljc"]
     :figwheel {:on-jsload binasoalan.core/mount-root}
     :compiler {:main binasoalan.core
                :closure-defines {"day8.re_frame.tracing.trace_enabled_QMARK_" true
                                  "re_frame.trace.trace_enabled_QMARK_" true}
                :output-to "resources/public/js/compiled/app.js"
                :output-dir "resources/public/js/compiled/out"
                :preloads [day8.re-frame-10x.preload]
                :asset-path "js/compiled/out"}}
    {:id "min"
     :source-paths ["src/cljs" "src/cljc"]
     :compiler {:main binasoalan.core
                :closure-defines {goog.DEBUG false}
                :output-to "resources/public/js/compiled/app.js"
                :optimizations :advanced
                :pretty-print false}}]}

  :profiles
  {:dev [:project/dev :profiles/dev]

   :test [:project/test :profiles/test]

   :prod {:dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]}

   :uberjar {:aot [binasoalan.server]}

   :project/dev {:dependencies [[day8.re-frame/re-frame-10x "0.3.3-react16"]
                                [day8.re-frame/tracing "0.5.1"]
                                [io.pedestal/pedestal.service-tools "0.5.3"]]

                 :plugins [[lein-figwheel "0.5.16"
                            :exclusions [org.clojure/clojure]]]}

   :project/test {:dependencies [[etaoin "0.2.8-SNAPSHOT"]]}}

  :aliases {"build" ["do" "clean" ["cljsbuild" "once" "min"] "uberjar"]})
