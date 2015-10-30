(ns semvera.util
  #?(:clj
     (:require [clojure.edn :as edn]))
  #?(:cljs (:require [cljs.tools.reader.edn :as edn]))
  #?(:clj
     (:import [java.util.regex Pattern Matcher])))

(defn regex [re]
  #?(:clj  (Pattern/compile re)
     :cljs (js/RegExp re)))

(defn parse-natural-number
  "Parses a string to a natural number. Returns `nil` if not given
  valid input, unless `:throws-exception` is set. This function also
  automatically promotes to `BigInt` if needed."
  [s & {:keys [throw-exceptions] :as opts}]
  (when (or throw-exceptions
            (and s (re-matches #"\d+" s)))
    (edn/read-string s)))

#?(:clj
   (defmacro compare-chain
     "This is a convenience macro for writing comparators with complex
     precedence rules.

     Continues comparing down the chain until a comparison returns
     not equal (a value other than 0 or nil). Otherwise, the two
     values are considered equal and 0 is returned."
     ([] 0)
     ([x]
      `(let [x# ~x]
         (if (nil? x#) 0 x#)))
     ([x & next]
      `(let [x# ~x]
         (if (or (nil? x#) (zero? x#))
           (compare-chain ~@next)
           x#)))))

; Note: This function does not return a lazy sequence because it's not
; possible to know whether the input string is valid or not until we
; reach the end of the string.
#?(:clj
   (defn re-linked
     "Returns a collection of successive matches of pattern in string,
      but allowing no gaps between the matches. In other words, the
      `end` index of the first match must be equal to the `start` index
      of the second match, and so on until there are no more matches left."
     [re s & {:keys [throw-exceptions] :as opts}]
     (let [m (re-matcher re s)
           result (loop [start 0, result nil]
                    (if (.find m)
                      (if (= start (.start m))
                        (recur
                          (.end m)
                          (conj result (re-groups m)))
                        start)
                      result))]
       (if (or (nil? result) (number? result))
         (when throw-exceptions (throw (Exception. (str "String did not match pattern at: " (or result 0)))))
         result))))

#?(:cljs
   (defn re-linked [re s & {:keys [throw-exceptions] :as opts}]
     (let [matcher (js/RegExp (.-source re) "g")
           result (loop [start 0, result nil]
                    (let [m (.exec matcher s)]
                      (if m
                        (if (= start (.-index m))
                          (recur
                            (.-lastIndex matcher)
                            (conj result m))
                          start)
                        result)))]
       (if (or (nil? result) (number? result))
         (when throw-exceptions (throw (str "String did not match pattern at: " (or result 0))))
         result))))

(defn throw-illegal-argument-exception [message]
  #?(:clj (throw (IllegalArgumentException. message))
     :cljs (throw (js/Error. message))))