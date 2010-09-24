(ns lazytest.main
  "Command-line test launcher."
  (:gen-class)
  (:use lazytest.results
	lazytest.tracker
	lazytest.runner.console
	lazytest.report.nested
	[clojure.java.io :only (file)]))


(defn -main
  "Run with directories as arguments.  Runs all tests in those
  directories; returns 0 if all tests pass."
  [& dirnames]
  (let [namespaces ((tracker (map file dirnames) 0))]
    (apply require namespaces)
    (let [results (apply run-tests namespaces)]
      (report results)
      (System/exit (summary-exit-value (summarize results))))))
