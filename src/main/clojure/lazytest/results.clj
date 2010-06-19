(ns lazytest.results
  "Any object encapsulating results of a single test must implement
  the TestResult protocol.  The record types defined here should be
  created with the constructor functions pass, fail, throw, skip, and
  pending."
  (:use [lazytest.arguments :only (nil-or)]))

(defprotocol TestResult
  (success? [r] "True if this result and all its children passed.")
  (pending? [r] "True if this is the result of an empty test.")
  (error? [r] "True if this is the result of a thrown exception.")
  (skipped? [r] "True if this is the result of a skipped test.")
  (container? [r] "True if this is a container for other results."))

(defrecord TestResultContainer [source children]
  TestResult
    (success? [this] (every? success? children))
    (pending? [this] (if (seq children) false true))
    (error? [this] false)
    (skipped? [this] false)
    (container? [this] true))

(defrecord TestPassed [source states]
  TestResult
    (success? [this] true)
    (pending? [this] false)
    (error? [this] false)
    (skipped? [this] false)
    (container? [this] false))

(defrecord TestFailed [source states]
  TestResult
    (success? [this] false)
    (pending? [this] false)
    (error? [this] false)
    (skipped? [this] false)
    (container? [this] false))

(defrecord TestThrown [source states throwable]
  TestResult
    (success? [this] false)
    (pending? [this] false)
    (error? [this] true)
    (skipped? [this] false)
    (container? [this] false))

(defrecord TestSkipped [source reason]
  TestResult
    (success? [this] true)
    (pending? [this] false)
    (error? [this] false)
    (skipped? [this] true)
    (container? [this] false))

(defrecord TestPending [source reason]
  TestResult
    (success? [this] true)
    (pending? [this] true)
    (error? [this] false)
    (skipped? [this] false)
    (container? [this] false))

(defrecord TestResultContainer [source children]
  TestResult
  (success? [this] (every? success? children))
  (pending? [this] false)
  (error? [this] false)
  (skipped? [this] false)
  (container? [this] true))

(defn container [source children]
  (TestResultContainer. source children))

(defn pass [source states]
  (TestPassed. source states))

(defn fail [source states]
  (TestFailed. source states))

(defn thrown [source states throwable]
  {:pre [(instance? Throwable throwable)]}
  (TestThrown. source states throwable))

(defn skip
  ([source] (skip source nil))
  ([source reason]
     {:pre [(nil-or string? reason)]}
     (TestSkipped. source reason)))

(defn pending
  ([source] (pending source nil))
  ([source reason]
     {:pre [(nil-or string? reason)]}
     (TestPending. source reason)))
