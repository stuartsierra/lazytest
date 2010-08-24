(ns lazytest.runtests)

(doseq [sym '[lazytest.expect-asserts
	      lazytest.context.stub-asserts]]
  (println "Running assertions in" sym)
  (require sym))

(println "All assertions passed.")