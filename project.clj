(defproject cltetris "0.1.0-SNAPSHOT"
  :main cltetris.core
  :description "May one day be a tetris game"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [org.clojure/clojurescript "0.0-2080"]]

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

  :cljsbuild {:builds [{:source-paths ["src/cljs" "target/generated/cljs"]
                        :compiler {:output-to "web/cltetris.js"
                                   :optimizations :simple
                                   :pretty-print true}}]})
