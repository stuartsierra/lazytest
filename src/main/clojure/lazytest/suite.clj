(ns lazytest.suite)

(defn suite
  "Sets metadata on function f identifying it as a test suite.  A test
  suite function must be free of side effects and must return a
  sequence of test cases and/or other test suites.

  The sequence returned by a test suite function may have before/after
  metadata (see lazytest.wrap).  'before' functions must be executed
  *before* all test case functions contained within the suite.
  'after' functions must be executed *after* all test case functions
  contained within the suite."
  [f]
  {:pre [(fn? f)]}
  (vary-meta f assoc ::suite true))

(defn suite?
  "True if x is a test suite."
  [x]
  (and (fn? x) (::suite (meta x))))
