(ns lazytest.runnable-test
  (:use [lazytest.test-result :only (pass fail thrown skip pending)])
  (:import (lazytest ExpectationFailed)))

(defprotocol RunnableTest
  (run-tests [this]
    "Runs tests and returns a seq of TestResult objects. Handles
    the :skip and :pending metadata flags."))

(defn runnable-test? [x]
  (extends? RunnableTest (type x)))

(defn skip-or-pending
  "If RunnableTest t has :skip or :pending metadata, returns the
  appropriate TestResult; else returns nil."
  [t]
  (if-let [reason (:skip (meta t))]
    (skip t reason)
    (if-let [reason (:pending (meta t))]
      (pending t reason)
      nil)))

(defmacro try-expectations
  "Executes body, catching all exceptions.  Returns a TestResult
  indicating pass, failure, or thrown."
  [t & body]
  `(try ~@body
	(pass ~t)
	(catch ExpectationFailed e#
	  (fail ~t (.reason e#)))
	(catch Throwable e#
	  (thrown ~t e#))))
