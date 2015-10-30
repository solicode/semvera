(ns semvera.test.runner
  (:require [cljs.test :as t]))

(enable-console-print!)

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (println "CLJS tests failed.")
    (.exit js/process 1)))

(defn -main [& args]
  (t/run-all-tests #"semvera\.test\..+"))

(set! *main-cli-fn* -main)
