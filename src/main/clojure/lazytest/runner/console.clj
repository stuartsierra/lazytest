(ns lazytest.runner.console
  (:use lazytest.find
	lazytest.suite
	lazytest.test-case
	lazytest.focus
	lazytest.wrap)
  (:import lazytest.ExpectationFailed))

(defn identifier [x]
  (let [m (meta x)]
    (str (or (:name m)
	     (:doc m)
	     (System/identityHashCode x))
	 " (" (:file m) ":" (:doc m) ")")))

(defn run-test-case [tc]
  (println "Running test case" (identifier tc))
  (do-before tc)
  (let [result (try-test-case tc)]
    (do-after tc)
    result))

(defn run-suite [suite]
  (let [suite-seq (suite)]
    (println "Running suite" (identifier suite-seq))
    (do-before suite-seq)
    (let [results (doall (map (fn [x]
				(cond (suite? x) (run-suite x)
				      (test-case? x) (run-test-case x)
				      :else (throw (IllegalArgumentException.
						    "Non-test given to run-suite."))))
			      suite-seq))]
      (do-after suite-seq)
      results)))

(defn run-tests
  [& namespaces]
  (let [nns (if (seq namespaces) namespaces (all-ns))
	suites (remove nil? (map find-tests namespaces))]
    (doall (map run-suite suites))))
