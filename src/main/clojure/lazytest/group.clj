(ns lazytest.group
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable-test :only (RunnableTest
				       run-tests
				       skip-or-pending
				       runnable-test?)]
	[lazytest.fixture :only (setup teardown fixture?)]
	[lazytest.result :only (pass fail thrown result-group)])
  (:import (lazytest ExpectationFailed)))

(comment
  (defn- debug [& args]
    (spit "log" 
	  (str (pr-str args) "\n")
	  :append true)))

(defmacro debug [& args] nil)

(defn- apply-test-case [tc arguments]
  (debug "apply-test-case" tc arguments)
  (let [this (vary-meta tc assoc :states arguments)]
    (try (apply (:f tc) arguments)
	 (pass this)
	 (catch ExpectationFailed e (fail this (.reason e)))
	 (catch Throwable e (thrown this e)))))

(defn- run-test-case [tc]
  (debug "run-test-case" tc)
  (try (let [states (map setup (:fixtures tc))
	     result (apply-test-case tc states)]
	 (dorun (map teardown (:fixtures tc)))
	 result)
       (catch Throwable e (thrown tc e))))

(defn- run-test-sequence [tc]
  (debug "run-test-sequence" tc)
  (try 
    (let [state-sequences (map (fn [fix]
				 (debug "Mapping over fixture" fix)
				 (if (:sequential (meta fix))
				   (do (debug "sequential fixture" fix)
				       (setup fix))
				   (do (debug "singular fixture" fix)
				       (repeatedly (fn [] (setup fix))))))
			       (:fixtures tc))
	  _unused_ (debug "state-sequences" state-sequences)
	  argument-lists (apply map list state-sequences)]
      (debug "argument-lists" argument-lists)
      (map (partial apply-test-case tc) argument-lists))
    (catch Throwable e (list (thrown tc e)))))

(defrecord TestCase [fixtures f]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (debug "TestCase.run-tests" this)
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

(defrecord TestGroup [children]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (debug "TestGroup.run-tests" this)
	     (lazy-seq
	      (list
	       (or (skip-or-pending this)
		   (result-group this (lazy-seq (mapcat run-tests children))))))))

(defn test-group
  "Creates a group of tests.  children are test cases or nested groups."
  ([children] (test-group children nil))
  ([children metadata]
     {:pre [(every? runnable-test? children)]}
     (TestGroup. children metadata nil)))
