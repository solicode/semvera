(ns semvera.test.macros)

(defn- cljs-env? [env]
  (boolean (:ns env)))

(defmacro if-cljs
  "Return `then` if we are generating cljs code and `else` for Clojure code.
   https://groups.google.com/d/msg/clojurescript/iBY5HaQda4A/w1lAQi9_AwsJ"
  [then else]
  (if (cljs-env? &env) then else))

(defmacro exception-thrown? [& body]
  `(if-cljs
     (try
       ~@body
       false
       (catch :default e#
         true))
     (try
       ~@body
       false
       (catch Exception e#
         true))))
