(defproject binasoalan "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :min-lein-version "2.7.0"

  :dependencies [[buddy "2.0.0"]
                 [cljs-ajax "0.7.3"]
                 [compojure "1.6.1"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [org.clojure/core.async "0.4.474"]
                 [org.eclipse.jetty/jetty-server "9.4.9.v20180320"]
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]
                 [ring/ring-anti-forgery "1.2.0"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.5.0-beta1"]
                 [secretary "1.2.3"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-ring "0.12.4"]]

  :source-paths ["src/clj"]

  :resource-paths ["resources"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :ring {:handler binasoalan.handler/app
         :async? true}

  :figwheel {:css-dirs ["resources/public/css"]}

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src/cljs"]
     :figwheel {:on-jsload binasoalan.core/mount-root}
     :compiler {:main binasoalan.core
                :closure-defines {"day8.re_frame.tracing.trace_enabled_QMARK_" true
                                  "re_frame.trace.trace_enabled_QMARK_" true}
                :output-to "resources/public/js/compiled/app.js"
                :output-dir "resources/public/js/compiled/out"
                :preloads [day8.re-frame-10x.preload]
                :asset-path "js/compiled/out"}}
    {:id "min"
     :source-paths ["src/cljs"]
     :compiler {:main binasoalan.core
                :closure-defines {goog.DEBUG false}
                :output-to "resources/public/js/compiled/app.js"
                :optimizations :advanced
                :pretty-print false}}]}

  :profiles
  {:dev {:dependencies [[day8.re-frame/re-frame-10x "0.3.3-react16"]
                        [day8.re-frame/tracing "0.5.1"]
                        [javax.servlet/servlet-api "2.5"]
                        [org.slf4j/slf4j-nop "1.7.13" :scope "test"]
                        [ring/ring-mock "0.3.2"]]

         :plugins [[lein-figwheel "0.5.16"]]}
   :prod {:dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]}}

  :auto-clean false

  :aliases {"build" ["do" "clean" ["cljsbuild" "once" "min"] ["ring" "uberjar"]]})
