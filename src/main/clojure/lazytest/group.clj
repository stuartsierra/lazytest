(ns lazytest.group
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable-test :only (RunnableTest
				       run-tests
				       skip-or-pending
				       runnable-test?)]
	[lazytest.fixture :only (setup teardown fixture?)]
	[lazytest.test-result :only (pass fail thrown result-group)])
  (:import (lazytest ExpectationFailed)))

(defrecord TestCase [fixtures f]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (lazy-seq
	      (list
	       (or (skip-or-pending this)
		   (try (let [states (map setup fixtures)
			      this-with-state (vary-meta this assoc :states states)]
			  (try (apply f states)
			       (pass this-with-state)
			       (catch ExpectationFailed e (fail this-with-state (.reason e)))
			       (catch Throwable e (thrown this-with-state e))))
			(catch Throwable e (thrown this e))))))))

(defn test-case
  ([fixtures f] (test-case fixtures f nil))
  ([fixtures f metadata]
     {:pre [(every? fixture? fixtures) (fn? f)]}
     (TestCase. fixtures f metadata nil)))

(defrecord TestGroup [children]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (lazy-seq
	      (list
	       (or (skip-or-pending this)
		   (result-group this (lazy-seq (mapcat run-tests children))))))))

(defn test-group
  ([children] (test-group children nil))
  ([children metadata]
     {:pre [(every? runnable-test? children)]}
     (TestGroup. children metadata nil)))
