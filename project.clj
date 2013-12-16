(defproject cltetris "0.1.0-SNAPSHOT"
  :main cltetris.runner
  :description "Tetris clone in clojure, clojurescript"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/clojurescript "0.0-2122"]
                 [om "0.1.0"]]

  :plugins [[com.keminglabs/cljx "0.3.1"]
            [lein-cljsbuild "1.0.1-SNAPSHOT"]]

  :source-paths ["src/clj" "target/generated/clj"]

  :hooks [cljx.hooks]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/generated/clj"
                   :rules :clj}

                  {:source-paths ["src/cljx"]
                   :output-path "target/generated/cljs"
                   :rules :cljs}]}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs" "target/generated/cljs"]
                        :compiler {:output-to "web/cltetris.js"
                                   :output-dir "web/out"
                                   :optimizations :none
                                   :source-map true
                                   :foreign-libs [{:file "om/react.js"
                                                   :provides ["React"]}]
                                   :externs ["om/externs/react.js"]}}
                       {:id "prod"
                        :source-paths ["src/cljs" "target/generated/cljs"]
                        :compiler {:output-to "web/cltetris.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]})
