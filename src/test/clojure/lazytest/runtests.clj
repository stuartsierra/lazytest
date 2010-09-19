(ns lazytest.runtests
  (:require lazytest.runner.console
	    lazytest.report.nested))

(doseq [sym '[lazytest.expect-asserts
	      lazytest.context.stub-asserts
	      lazytest.context.file-asserts]]
  (println "Running assertions in" sym)
  (require sym))

(println "All assertions passed.")

(doseq [sym '[examples.suite1
	      examples.describe1
	      examples.random-test
	      examples.multimethods]]
  (println "Loading" sym)
  (require sym)
  (println "Running tests in" sym)
  (lazytest.report.nested/report
   (lazytest.runner.console/run-tests sym)))


(println "Loading examples.readme")
(require 'examples.readme)
(println "Running tests in examples.readme")
(lazytest.report.nested/report
 (apply
  lazytest.runner.console/run-tests
  (filter #(.startsWith (name %) "examples.readme")
	  (map ns-name (all-ns)))))

