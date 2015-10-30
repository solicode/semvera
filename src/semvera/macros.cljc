(ns semvera.macros
  (:require [clojure.string :as s])
  #?(:clj (:import [java.util.regex Pattern])))

#?(:clj
   (defmacro minify-regex [re]
     (cond
       (or (string? re) (instance? Pattern re))
       (s/replace (str re) #"((?<!\\)#.*)|(\s+)" "")

       :else
       (throw (IllegalArgumentException. "Must be a regex or string constant in order to compile regex")))))
