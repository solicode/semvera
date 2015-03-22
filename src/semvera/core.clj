(ns semvera.core
  (:require [clojure.string :as s]
            [semvera.util :refer :all]))

(defn- compare-ids [x y]
  (let [x-num (parse-natural-number x)
        y-num (parse-natural-number y)]
    ; Numeric indentifier are considered 'less than' alphanumeric ones. And if they are the same type,
    ; then order them naturally (using the default comparators).
    (compare-chain
      (when (and x-num (not y-num)) -1)
      (when (and y-num (not x-num)) 1)
      (compare (or x-num x) (or y-num y)))))

(defn- compare-pre-release [x y]
  (let [x-ids (when x (s/split x #"\."))
        y-ids (when y (s/split y #"\."))]
    (compare-chain
      (->> (map compare-ids x-ids y-ids)
        (remove zero?)
        (first))
      (compare (count x-ids) (count y-ids)))))

(defrecord SemVer [major minor patch pre-release build]
  Comparable
  (compareTo [_ other]
    (compare-chain
      (compare major (:major other))
      (compare minor (:minor other))
      (compare patch (:patch other))
      (when (and pre-release (not (:pre-release other))) -1)
      (when (and (not pre-release) (:pre-release other)) 1)
      (compare-pre-release pre-release (:pre-release other))))

  Object
  (toString [_]
    (str major \. minor \. patch
         (when pre-release (str \- pre-release))
         (when build (str \+ build)))))

(defrecord SemVerConstraint [op version]
  Object
  (toString [_]
    (str (name op) version)))

(defrecord SemVerRange [constraint-groups]
  Object
  (toString [_]
    (if (every? empty? constraint-groups)
      "*"
      (s/join " || " (map #(s/join " " %) constraint-groups)))))

(def semver-pattern #"(?x)
\s*
v?=?                                        # Allow `v` and `=` prefixes, though they will be ignored.
\s*
([0-9]+)                                    ## Major
(?:\.([0-9]+))                              ## Minor
(?:\.([0-9]+))                              ## Patch
(?:-?([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?  ## Pre-release
(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?  ## Build
\s*
")

; Note: At least one space between hyphen for range required (otherwise it would be
; amibiguous, since pre-release is also separated with a hyphen)
(def semver-range-pattern #"(?x)
\s*
(~|~>|\^|<|<=|>|>=|=)?                      ## Operation
\s*
v?                                          # Allow `v` in range, but it will be ignored
\s*
([0-9]+|[Xx\*])                             ## Major
(?:\.([0-9]+|[Xx\*]))?                      ## Minor
(?:\.([0-9]+|[Xx\*]))?                      ## Patch
(?:-?([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?  ## Pre-release
(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?  ## Build

(?:
\s+-\s+                                     # hyphen separating lower and upper part of range
([0-9]+)                                    ## Major
(?:\.([0-9]+))?                             ## Minor
(?:\.([0-9]+))?                             ## Patch
(?:-?([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?  ## Pre-release
(?:\+([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?  ## Build
)?
\s*
")

;; Default comparators in core only work on numbers, so here are their comparator equivalents:

(defn <'
  ([x] true)
  ([x y] (neg? (compare x y)))
  ([x y & more]
   (if (<' x y)
     (if (next more)
       (recur y (first more) (next more))
       (<' y (first more)))
     false)))

(defn <='
  ([x] true)
  ([x y] (<= (compare x y) 0))
  ([x y & more]
   (if (<=' x y)
     (if (next more)
       (recur y (first more) (next more))
       (<=' y (first more)))
     false)))

(defn >'
  ([x] true)
  ([x y] (pos? (compare x y)))
  ([x y & more]
   (if (>' x y)
     (if (next more)
       (recur y (first more) (next more))
       (>' y (first more)))
     false)))

(defn >='
  ([x] true)
  ([x y] (>= (compare x y) 0))
  ([x y & more]
   (if (>=' x y)
     (if (next more)
       (recur y (first more) (next more))
       (>=' y (first more)))
     false)))

(defn ='
  ([x] true)
  ([x y] (zero? (compare x y)))
  ([x y & more]
   (if (=' x y)
     (if (next more)
       (recur y (first more) (next more))
       (=' y (first more)))
     false)))

(defn not='
  ([x] true)
  ([x y] (not (zero? (compare x y))))
  ([x y & more]
   (if (not=' x y)
     (if (next more)
       (recur y (first more) (next more))
       (not=' y (first more)))
     false)))

(defn semver?
  "Return `true` if the string is a valid `semver`, otherwise `false`."
  [s]
  (boolean (re-matches semver-pattern s)))

(defn semver-range?
  "Return `true` if the string is a valid `semver-range`, otherwise `false`."
  [s]
  (every? boolean
    (for [group (s/split s #"\|\|" -1)]
      (re-linked semver-range-pattern group))))

(defn semver
  "Creates a `SemVer` record from a string. Returns `nil` if the input is invalid, unless `:throws-exception` is
  explicity set in `opts`."
  [s & {:keys [throw-exceptions] :as opts}]
  (let [[matched? major minor patch pre-release build] (re-matches semver-pattern s)]
    (if matched?
      (->SemVer
        (parse-natural-number major)
        (parse-natural-number minor)
        (parse-natural-number patch)
        pre-release
        build)
      (when throw-exceptions (throw (IllegalArgumentException. (str "'" s "' is not a valid semantic version.")))))))

(defn- part->num [part]
  (case part
    ("x" "X" "*" nil) 0
    (parse-natural-number part)))

(defn- x-range? [part]
  (case part
    ("x" "X" "*" nil) true
    false))

(defn- comparator? [part]
  (case part
    ("<" ">" "<=" ">=") true
    false))

(defn- passes? [constraint version]
  {:pre [(instance? SemVer version)]}
  (case (:op constraint)
    := (=' version (:version constraint))
    :< (<' version (:version constraint))
    :> (>' version (:version constraint))
    :<= (<=' version (:version constraint))
    :>= (>=' version (:version constraint))))

(defn same-version-number?
  "Returns `true` if the two versions have the same [`major` `minor` `patch`], otherwise `false`."
  [ver1 ver2]
  (and (= (:major ver1) (:major ver2))
       (= (:minor ver1) (:minor ver2))
       (= (:patch ver1) (:patch ver2))))

(defn- passes-all? [constraints version]
  (when (or (empty? constraints) ; Empty constraints is equivalent to `*`, meaning everything passes.
            (not (:pre-release version))
            ; If we're dealing with a pre-release version, only compare it against the range if
            ; there is at least one constraint with that same pre-release version AND if it has
            ; the same `[major minor patch]`.
            ;
            ; This is important because it's not reasonable to give the user a pre-release version
            ; for a version number that they didn't specify. There are risks with using pre-releases,
            ; so we only allow them if the user has clearly indicated their intent.
            (and (:pre-release version)
                 (some #(and (get-in % [:version :pre-release])
                             (same-version-number? (:version %) version))
                   constraints)))
    (every? true?
      (map #(passes? % version) constraints))))

(defn in-range?
  "Returns `true` if the version falls within the specified range, otherwise `false`."
  [range version]
  (->> (:constraint-groups range)
    (map #(passes-all? % version))
    (filter true?)
    (first)
    (boolean)))

(defn- expand-version-number [major minor patch]
  (cond
    (x-range? major) [:any :any :any]
    (x-range? minor) [(inc (part->num major)) 0 0]
    (x-range? patch) [(part->num major) (inc (part->num minor)) 0]
    :else [(part->num major) (part->num minor) (part->num patch)]))

(defn- str->constraint-groups [s]
  (for [group (s/split s #"\|\|" -1)
        :let [ranges (re-linked semver-range-pattern group)]]
    (if-not ranges
      :invalid-range
      (flatten
        (for [range ranges]
          (let [[_ op major minor patch pre-release build, major2 minor2 patch2 pre-release2 build2] range
                major' (part->num major)
                minor' (part->num minor)
                patch' (part->num patch)
                upper (cond
                        major2
                        (cond (not minor2)
                              (->SemVerConstraint :< (->SemVer (inc (part->num major2)) (part->num minor2) 0 pre-release2 build2))

                              (not patch2)
                              (->SemVerConstraint :< (->SemVer (part->num major2) (inc (part->num minor2)) (part->num patch2) pre-release2 build2))

                              :else
                              (->SemVerConstraint :<= (->SemVer (part->num major2) (part->num minor2) (part->num patch2) pre-release2 build2)))

                        (and (= op "^") (pos? major'))
                        (->SemVerConstraint :< (->SemVer (inc major') 0 0 pre-release2 build2))

                        (and (= op "^") (pos? minor'))
                        (->SemVerConstraint :< (->SemVer major' (inc minor') 0 pre-release2 build2))

                        (and (= op "^") (pos? patch'))
                        (->SemVerConstraint :< (->SemVer major' minor' (inc patch') pre-release2 build2))

                        (and (not (comparator? op)) (x-range? major))
                        :any

                        (and (not (comparator? op)) (x-range? minor))
                        (->SemVerConstraint :< (->SemVer (inc major') 0 0 pre-release2 build2))

                        (and (not (comparator? op)) (x-range? patch))
                        (->SemVerConstraint :< (->SemVer major' (inc minor') 0 pre-release2 build2))

                        (and (or (= op "~") (= op "~>")) patch)
                        (->SemVerConstraint :< (->SemVer major' (inc minor') 0 pre-release2 build2)))]
            (cond
              (= upper :any)
              []

              upper
              [(->SemVerConstraint :>= (->SemVer (part->num major) (part->num minor) (part->num patch) pre-release build))
               upper]

              :else
              [(cond (or (= op ">=") (= op "<"))
                     (->SemVerConstraint (keyword op) (->SemVer major' minor' patch' pre-release build))

                     (= op "<=")
                     (if (or (x-range? minor) (x-range? patch))
                       (->SemVerConstraint :< (apply ->SemVer
                                                (conj (expand-version-number major minor patch)
                                                  pre-release
                                                  build)))
                       (->SemVerConstraint :<= (->SemVer major' minor' patch' pre-release build)))

                     (= op ">")
                     (cond (x-range? minor)
                           (->SemVerConstraint :>= (->SemVer (inc major') minor' patch' pre-release build))

                           (x-range? patch)
                           (->SemVerConstraint :>= (->SemVer major' (inc minor') patch' pre-release build))

                           :else
                           (->SemVerConstraint :> (->SemVer major' minor' patch' pre-release build)))

                     :else
                     (->SemVerConstraint := (->SemVer (part->num major) (part->num minor) (part->num patch) pre-release build)))])))))))

(defn semver-range
  "Creates a `SemVerRange` record from a string. Returns `nil` if the input is invalid, unless `:throws-exception` is
  explicity set in `opts`."
  [s & {:keys [throw-exceptions] :as opts}]
  (let [constraint-groups (str->constraint-groups s)]
    (if (some #(= % :invalid-range) constraint-groups)
      (when throw-exceptions (throw (IllegalArgumentException. (str "'" s "' is not a valid semantic version range."))))
      (->SemVerRange constraint-groups))))
