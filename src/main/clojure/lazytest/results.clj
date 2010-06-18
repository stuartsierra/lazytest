(ns lazytest.results
  (:use [lazytest.plan :only (example?)]))

(defprotocol TestResult
  (success? [r] "True if this result and all its children passed.")
  (pending? [r] "True if this is the result of an empty test.")
  (error? [r] "True if this is the result of a thrown exception.")
  (container? [r] "True if this is a container for other results."))

(defrecord TestResultContainer [source children]
  TestResult
    (success? [this] (every? success? children))
    (pending? [this] (if (seq children) false true))
    (error? [this] false)
    (container? [this] true))

(defrecord TestPassed [source states]
  TestResult
    (success? [this] true)
    (pending? [this] false)
    (error? [this] false)
    (container? [this] false))

(defrecord TestFailed [source states]
  TestResult
    (success? [this] false)
    (pending? [this] false)
    (error? [this] false)
    (container? [this] false))

(defrecord TestThrown [source states throwable]
  TestResult
    (success? [this] false)
    (pending? [this] false)
    (error? [this] true)
    (container? [this] false))

(defn pass [source states]
  {:pre [(example? source)]}
  (TestPassed. source states))

(defn fail [source states]
  {:pre [(example? source)]}
  (TestFailed. source states))

(defn thrown [source states throwable]
  {:pre [(example? source)
	 (instance? Throwable throwable)]}
  (TestThrown. source states throwable))
