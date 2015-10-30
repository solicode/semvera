(defproject net.solicode/semvera "0.1.0-SNAPSHOT"
  :description "Semvera is a semantic version parser with npm-like range syntax support."
  :url "https://github.com/solicode/semvera"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :main semvera.core
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.145"]]
  :aliases {"test-all" ["do" "clean," "test," "cljsbuild" "test"]}
  :plugins [[lein-cljsbuild "1.1.0"]]
  :cljsbuild {:builds        [{:id           "test"
                               :source-paths ["src" "test"]
                               :compiler     {:output-to     "target/cljs/test-runner.js"
                                              :output-dir    "target/cljs"
                                              :target        :nodejs
                                              :optimizations :simple}}]
              :test-commands {"test" ["node" "target/cljs/test-runner.js"]}}
  :profiles {:dev {:jvm-opts ["-XX:-OmitStackTraceInFastThrow"]}})
