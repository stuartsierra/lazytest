(ns lazytest.runtests)

(doseq [sym '[lazytest.expect-asserts
	      lazytest.fixture-asserts
	      lazytest.fixture.stub-asserts]]
  (println "Running assertions in" sym)
  (require sym))

(println "All assertions passed.")