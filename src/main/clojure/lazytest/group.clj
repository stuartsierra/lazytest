(ns lazytest.group)

(defn RunnableExample [fixtures f]
  RunnableTest
  (run-tests [this]
	     (or (skip-or-pending this)
		 (try-expectations this
				   (apply f (map setup fixtures))
				   (dorun (map teardown fixtures))))))

(defrecord Example [locals fixtures expr]
  Testable
  (get-tests [this]
	     (RunnableExample. fixtures (eval `(fn ~locals ~expr)))))

(defrecord SimpleFixture [value]
  Fixture
  (setup [this] value)
  (teardown [this] nil))

(defrecord Group [children]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this] (result-group this (mapcat run-tests children))))

(defrecord BindingFixtureGroup [locals fixtures children]
  Testable
  (get-tests [this]
	     (mapcat (fn [child]
		       (get-tests
			(assoc child
			  :locals (vec (concat (:locals this) (:locals child)))
			  :fixtures (vec (concat (:locals this) (:locals child))))))
		     children)))

(defrecord MappingGroup [local sequence expr]
  (get-tests [this]
	     (map (fn [value] (Example. [local] [(SimpleFixture. x)] expr))
		  sequence)))

