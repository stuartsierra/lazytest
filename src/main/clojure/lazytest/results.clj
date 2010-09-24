(ns lazytest.results
  (:use lazytest.suite))

(defn result-seq
  "Given a single suite result, returns a depth-first sequence of all
  nested child suite/test results."
  [result]
  (tree-seq suite-result? :children result))

(defn summarize
  "Given a sequence of suite results, returns a map of counts with
  keys :total, :pass, and :fail."
  [results]
  (let [test-case-results (remove suite-result? (mapcat result-seq results))
	total (count test-case-results)
	passed (count (filter :pass? test-case-results))
	failed (- total passed)]
    {:total total, :pass passed, :fail failed}))

(defn summary-exit-value 
  "Given a summary map as returned by summarize, returns 0 if there
  are no failures and -1 if there are."
  [summary]
  (if (zero? (:fail summary)) 0 -1))
