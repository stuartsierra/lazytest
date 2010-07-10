(ns lazytest.test-result)

(defprotocol TestResult
  "The result of run-tests on a RunnableTest.  Every object
  implementing TestResult should have a :source field containing the
  RunnableTest object that produced this result."
  
  (success? [this]
    "Returns true if this test (and all its sub-tests) succeeded.
    Skipped and pending tests count as successful."))


(defrecord Pass [source]
  TestResult
  (success? [this] true))

(defn pass [source]
  (Pass. source))


(defrecord Fail [source reason]
  TestResult
  (success? [this] false))

(defn fail [source reason]
  (Fail. source reason))


(defrecord Thrown [source throwable]
  TestResult
  (success? [this] false))

(defn thrown [source throwable]
  {:pre [(instance? Throwable throwable)]}
  (Thrown. source throwable))


(defrecord Skip [source reason]
  TestResult
  (success? [this] true))

(defn skip [source reason]
  (Skip. source reason))


(defrecord Pending [source reason]
  TestResult
  (success? [this] true))

(defn pending [source reason]
  (Pending. source reason))


(defrecord TestResultGroup [source children]
  TestResult
  (success? [this] (every? success? children)))

(defn result-group [source children]
  (TestResultGroup. source children))
