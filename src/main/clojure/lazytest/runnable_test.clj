(ns lazytest.runnable-test
  (:use [lazytest.result :only (skip pending)]))

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
