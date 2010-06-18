(ns lazytest.run
  (:use [lazytest.plan :only (example?)]
	[lazytest.contexts :only (open-context close-context)]
	[lazytest.results :only (pass fail thrown)]))

(defn run-example
  "Run a single RunnableExample and return its result."
  [ex]
  {:pre [(example? ex)]}
  (let [active (reduce open-context {} (:contexts ex))
	states (map active (:contexts ex))
	result (try (if (apply (:f ex) states)
		      (pass ex states)
		      (fail ex states))
		    (catch Throwable t (thrown ex states t)))]
    (reduce close-context active (:contexts ex))
    result))

(defn run
  "Runs a collection of RunnableExamples in order."
  [exs]
  (map run-example exs))