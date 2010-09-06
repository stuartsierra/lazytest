(ns lazytest.runner.debug
  (:use lazytest.find
	lazytest.suite
	lazytest.test-case
	lazytest.focus
	lazytest.context
	[clojure.stacktrace :only (print-cause-trace)])
  (:import lazytest.ExpectationFailed))

(defn identifier [x]
  (let [m (meta x)]
    (str (or (:name m)
	     (:doc m)
	     (:nested-doc m)
	     (System/identityHashCode x))
	 " (" (:file m) ":" (:line m) ")")))

(defn run-test-case [tc]
  (println "Running test case" (identifier tc))
  (setup-contexts tc)
  (let [result (try-test-case tc)]
    (prn result)
    (when-let [t (:thrown result)]
      (when (instance? ExpectationFailed t)
	(prn (.reason t)))
      (print-cause-trace t 5))
    (teardown-contexts tc)
    (println "Done with test case" (identifier tc))
    result))

(defn run-suite [ste]
  (let [ste-seq (ste)]
    (println "Running suite" (identifier ste-seq))
    (setup-contexts ste-seq)
    (let [results (doall (map (fn [x]
				(cond (suite? x) (run-suite x)
				      (test-case? x) (run-test-case x)
				      :else (throw (IllegalArgumentException.
						    "Non-test given to run-suite."))))
			      ste-seq))]
      (teardown-contexts ste-seq)
      (println "Done with suite" (identifier ste-seq))
      (suite-result ste-seq results))))

(defn run-tests
  "Runs tests defined in the given namespaces, with verbose output."
  [& namespaces]
  (let [stes (apply find-suites namespaces)]
    (doall (map run-suite stes))))
