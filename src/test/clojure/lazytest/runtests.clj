(ns lazytest.runtests)

(doseq [sym '[lazytest.testable-asserts
	      lazytest.runnable-test-asserts
	      lazytest.expect-asserts
	      lazytest.fixture-asserts
	      lazytest.fixture.stub-asserts
	      lazytest.describe-asserts
	      lazytest.readme-examples]]
  (println "Running assertions in" sym)
  (require sym))

(println "All assertions passed.")