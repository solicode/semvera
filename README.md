Semvera [![Circle CI](https://circleci.com/gh/solicode/semvera.svg?style=svg)](https://circleci.com/gh/solicode/semvera)
=======

Semvera is a semantic version parser with range syntax support based on [node-semver](https://github.com/npm/node-semver).

*(Supports Clojure and ClojureScript)*

Overview
--------

Semvera parses text that conforms to the [semantic versioning specification](http://semver.org/) and gives back a Clojure record which you can use to perform higher level operations on, like extraction of individual components (`major`, `minor`, `patch`, etc), normalization, comparison, sorting, and so on.

Semvera also supports ranges (e.g. `1.1.0 - 1.5.0`, `~1.3.0`, `2.x`, `1.8.*`), which can be used to determine if a version is in a given range or not. The syntax for ranges is based on [node-semver](https://github.com/npm/node-semver), which is what [npm](https://www.npmjs.com) uses. It's also used in other package managers like [Cargo](https://github.com/rust-lang/cargo) for [Rust](http://www.rust-lang.org).

Getting Started
---------------

### Installation

Add the following dependency to your `project.clj` file:

```clojure
[net.solicode/semvera "0.1.0"]
```

### Examples

First include the `semavera.core` namespace:

```clojure
(require '[semvera.core :refer :all])
```

Now let's try a few things in the REPL:

```clojure
; Create a semver
(semver "1.1.0")
;=> #semvera.core.SemVer{:major 1, :minor 1, :patch 0, :pre-release nil, :build nil}

; Create a semver with pre-release and build info
(semver "2.0.0-alpha+340")
;=> #semvera.core.SemVer{:major 2, :minor 0, :patch 0, :pre-release "alpha",
; :build "340"}

; Compare semvers
(<' (semver "0.1.0") (semver "0.2.0"))
;=> true

; Or pass in more than 2 parameters. These comparison operators work much like
; the core operators do for numbers.
(<' (semver "0.1.0") (semver "0.2.0") (semver "0.3.0"))
;=> true

; SemVer implements Comparable, so you can sort them directly
(->> (map semver ["0.9.0" "1.0.0" "0.10.0" "0.2.0" "0.1.0" "0.1.0-SNAPSHOT"])
  (sort)
  (map str))
;=> ("0.1.0-SNAPSHOT" "0.1.0" "0.2.0" "0.9.0" "0.10.0" "1.0.0")

; Normalization is done automatically when converting back to a string
(str (semver "v1.0.0rc2"))
;=> "1.0.0-rc2"

; Check if a string is a semver
(semver? "not a semver")
=> false
```

#### Working with semver ranges

```clojure
; Create a semver-range and get a string representation of it
(str (semver-range "1.5.x"))
;=> ">=1.5.0 <1.6.0"

; Check if a semver is in a given semver-range
(in-range? (semver-range "1.2.0 - 1.5.0") (semver "2.5.0"))
;=> false

; You can combine rules with `||`
(in-range? (semver-range "1.2.0 - 1.5.0 || 2.x") (semver "2.5.0"))
;=> true

; Check if a string is a semver-range
(semver-range? "^3.1.8")
;=> true
```

Future Work
-----------

- Make a lot of these functions polymorphic using protocols? Consider accepting both `SemVer` and `String`.

License
-------

Copyright © 2015 Solicode

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
