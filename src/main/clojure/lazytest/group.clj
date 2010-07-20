(ns lazytest.group
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable-test :only (RunnableTest run-tests
				      skip-or-pending
				      try-expectations)]
	[lazytest.fixture :only (setup teardown constant-fixture)]
	[lazytest.test-result :only (result-group)]))

(defrecord RunnableExample [fixtures f]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (list
	      (or (skip-or-pending this)
		  (try-expectations
		   this
		   (apply f (map setup fixtures))
		   (dorun (map teardown fixtures)))))))

(defn inherit [parent child]
  (assoc child :fixtures (vec (concat (:fixtures parent) (:fixtures child)))))

(defrecord Group [fixtures children]
  Testable
  (get-tests [this]
	     (mapcat (fn [child] (get-tests (inherit this child)))
		     children))
  RunnableTest
  (run-tests [this] (result-group this (mapcat run-tests children))))

(defrecord MappingGroup [fixtures sequence children]
  Testable
  (get-tests [this]
	     (mapcat (fn [value]
		       (get-tests
			(Group.
			 (conj fixtures (constant-fixture value))
			 children)))
		     sequence)))

