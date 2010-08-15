(ns lazytest.runnable.test-group
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable :only (RunnableTest
				       run-tests
				       skip-or-pending
				       runnable?)]
	[lazytest.fixture :only (setup teardown fixture?)]
	[lazytest.result :only (pass fail thrown result-group)])
  (:import (lazytest ExpectationFailed)))

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
  "Creates a group of tests.  children are test cases or nested groups."
  ([children] (test-group children nil))
  ([children metadata]
     {:pre [(every? runnable? children)]}
     (TestGroup. children metadata nil)))
