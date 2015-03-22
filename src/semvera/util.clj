(ns semvera.util
  (:require [clojure.edn :as edn])
  (:import [java.util.regex Pattern Matcher]))

(defn parse-natural-number
  "Parses a string to a natural number. Returns `nil` if not given
  valid input, unless `:throws-exception` is set. This function also
  automatically promotes to `BigInt` if needed."
  [s & {:keys [throw-exceptions] :as opts}]
  (when (or throw-exceptions
            (and s (re-matches #"\d+" s)))
    (edn/read-string s)))

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
        x#))))

; Note: This function does not return a lazy sequence because it's not
; possible to know whether the input string is valid or not until we
; reach the end of the string.
(defn re-linked
  "Returns a collection of successive matches of pattern in string,
   but allowing no gaps between the matches. In other words, the
   `end` index of the first match must be equal to the `start` index
   of the second match, and so on until there are no more matches left."
  [^Pattern re s & {:keys [throw-exceptions] :as opts}]
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
      result)))
