(require '[cljs.repl]
         '[cljs.build.api]
         '[cljs.repl.node])

(cljs.build.api/build "src"
 {:output-to "target/cljs/main.js"
  :verbose   true})

(cljs.repl/repl (cljs.repl.node/repl-env)
 :watch "src"
 :output-dir "target/cljs")
