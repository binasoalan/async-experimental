(defproject binasoalan "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :min-lein-version "2.7.0"

  :dependencies [[buddy "2.0.0"]
                 [cheshire "5.8.0"]
                 [cljs-ajax "0.7.3"]
                 [com.draines/postal "2.0.2"]
                 [com.layerware/hugsql "0.4.8"]
                 [commons-codec "1.10"]
                 [compojure "1.6.1"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [enlive "1.1.6"]
                 [environ "1.1.0"]
                 [funcool/struct "1.2.0"]
                 [hiccup "1.0.5"]
                 [hikari-cp "2.4.0"]
                 [migratus "1.0.6"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [org.eclipse.jetty/jetty-server "9.4.9.v20180320"]
                 [org.postgresql/postgresql "42.2.2"]
                 [org.slf4j/slf4j-simple "1.7.25"]
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]
                 [ring "1.6.3"]
                 [ring/ring-anti-forgery "1.2.0"]
                 [ring/ring-defaults "0.3.1"]
                 [metosin/ring-http-response "0.9.0"]
                 [secretary "1.2.3"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-environ "1.1.0"]
            [lein-ring "0.12.4"
             :exclusions [org.clojure/clojure]]]

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/resources"]
  :test-paths ["test/clj"]
  :target-path "target/%s"
  :main ^:skip-aot binasoalan.migrations

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :auto-clean false

  :ring {:handler binasoalan.core/app
         :async? true
         :async-timeout 10000}

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

   :project/dev {:dependencies [[day8.re-frame/re-frame-10x "0.3.3-react16"]
                                [day8.re-frame/tracing "0.5.1"]]

                 :plugins [[lein-figwheel "0.5.16"
                            :exclusions [org.clojure/clojure]]]}

   :project/test {:dependencies [[etaoin "0.2.8-SNAPSHOT"]]}}

  :aliases {"build" ["do" "clean" ["cljsbuild" "once" "min"] ["ring" "uberjar"]]})
