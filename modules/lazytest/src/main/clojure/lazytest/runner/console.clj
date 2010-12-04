(ns lazytest.runner.console
  (:use lazytest.find
	lazytest.suite
	lazytest.test-case
	lazytest.focus
	lazytest.color)
  (:import lazytest.ExpectationFailed))

(defn- run-test-case [tc]
  (let [result (try-test-case tc)]
    (if (:pass? result)
      (print (colorize "." :green))
      (print (colorize "F" :red)))
    (flush)
    result))

(defn- run-test-seq [s]
  (let [results (doall (map (fn [x]
			      (cond (test-seq? x) (run-test-seq x)
				    (test-case? x) (run-test-case x)
				    :else (throw (IllegalArgumentException.
						  "Non-test given to run-suite."))))
			    s))]
    (suite-result s results)))

(defn run-tests
  "Runs tests defined in the given namespaces, with colored green dots
  indicating passing tests and red 'F's indicating falied tests."
  [& namespaces]
  (let [ste (apply find-suite namespaces)
	tree (filter-tree (expand-tree ste))]
    (when (focused? tree)
      (println "=== FOCUSED TESTS ONLY ==="))
    (let [result (run-test-seq tree)]
      (newline)
      result)))
