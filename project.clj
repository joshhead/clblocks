(defproject clblocks "0.1.0-SNAPSHOT"
  :main clblocks.runner
  :description "Falling blocks game in ClojureScript"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.319.0-6b1aca-alpha"]
                 [org.clojure/clojurescript "0.0-2280"]
                 [om "0.7.1"]]

  :plugins [[lein-cljsbuild "1.0.3"]]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "web/clblocks.js"
                                   :output-dir "web/out"
                                   :optimizations :none
                                   :source-map true}}
                       {:id "prod"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "web/clblocks.js"
                                   :optimizations :advanced
                                   :pretty-print false
                                   :preamble ["react/react.min.js"]
                                   :externs ["react/externs/react.js"]}}]})
