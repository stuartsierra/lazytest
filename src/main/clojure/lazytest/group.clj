(ns lazytest.group
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable-test :only (RunnableTest
				       run-tests
				       skip-or-pending
				       try-expectations
				       runnable-test?)]
	[lazytest.fixture :only (setup teardown fixture?)]
	[lazytest.test-result :only (result-group)]))

(defrecord TestCase [fixtures f]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (lazy-seq
	      (list
	       (or (skip-or-pending this)
		   (try-expectations
		    this
		    (apply f (map setup fixtures))
		    (dorun (map teardown fixtures))))))))

(defn test-case [fixtures f]
  {:pre [(every? fixture? fixtures) (fn? f)]}
  (TestCase. fixtures f))

(defrecord TestGroup [children]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (or (skip-or-pending this)
		 (result-group this (lazy-seq (mapcat run-tests children))))))

(defn test-group [children]
  {:pre [(every? (runnable-test? children))]}
  (TestGroup. children))