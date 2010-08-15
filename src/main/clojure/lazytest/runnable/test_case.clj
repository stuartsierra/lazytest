(ns lazytest.runnable.test-case
  (:use [lazytest.find :only (FindTests find-tests)]
	[lazytest.runnable :only (RunnableTest
				       run-tests
				       skip-or-pending
				       runnable?)]
	[lazytest.fixture :only (setup teardown fixture?)]
	[lazytest.result :only (pass fail thrown result-group)])
  (:import (lazytest ExpectationFailed)))

(defn- apply-test-case [tc arguments]
  (let [this (vary-meta tc assoc :states arguments)]
    (try (apply (:f tc) arguments)
	 (pass this)
	 (catch ExpectationFailed e (fail this (.reason e)))
	 (catch Throwable e (thrown this e)))))

(defn- run-test-case [tc]
  (try (let [states (map setup (:fixtures tc))
	     result (apply-test-case tc states)]
	 (dorun (map teardown (:fixtures tc)))
	 result)
       (catch Throwable e (thrown tc e))))

(defn- run-test-sequence [tc]
  (try 
    (let [state-sequences (map (fn [fix]
				 (if (:sequential (meta fix))
				   (setup fix)
				   (repeatedly (fn [] (setup fix)))))
			       (:fixtures tc))
	  argument-lists (apply map list state-sequences)]
      (map (partial apply-test-case tc) argument-lists))
    (catch Throwable e (list (thrown tc e)))))

(defrecord TestCase [fixtures f]
  FindTests
  (find-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (if-let [skipped (skip-or-pending this)]
	       (list skipped)
	       (if (some #(:sequential (meta %)) fixtures)
		 (run-test-sequence this)
		 (lazy-seq (list (run-test-case this)))))))

(defn test-case
  "Creates a test case.  fixtures is a vector of objects extending the
  Fixture protocol.  f is a function taking a number of arguments
  equal to the number of fixtures.  When 'run-tests' is called, the
  values returned by the 'before' methods of the fixtures will be
  passed as arguments to the function, in the same order."
  ([fixtures f] (test-case fixtures f nil))
  ([fixtures f metadata]
     {:pre [(every? fixture? fixtures) (fn? f)]}
     (TestCase. fixtures f metadata nil)))
