(ns lazytest.run)

(defprotocol RunnableTest
  (run-tests [t] "Runs test t and all its sub-tests, returns a TestResult"))

