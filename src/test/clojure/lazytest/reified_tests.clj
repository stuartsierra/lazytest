(ns lazytest.reified-tests
  (:use (lazytest expect testable runnable-test)))

(def t1 (reify Testable
	       (get-tests [this]
			  (list
			   (reify RunnableTest
				  (run-tests [this]
					     (list
					      (try-expectations this
								(expect= 1 2)))))))))

(def t2 (reify Testable
	       (get-tests [this]
			  (list
			   (reify RunnableTest
				  (run-tests [this]
					     (list
					      (try-expectations this
								(expect= 1 1)))))))))

(def t3 (reify Testable
	       (get-tests [this]
			  (list
			   (reify RunnableTest
				  (run-tests [this]
					     (list
					      (try-expectations this
								(expect-instance java.lang.String :a)))))))))

(def t4 (reify Testable
	       (get-tests [this]
			  (list
			   (reify RunnableTest
				  (run-tests [this]
					     (list
					      (try-expectations this
								(expect-not= 1 1)))))))))

(def t5 (reify Testable
	       (get-tests [this]
			  (list
			   (reify RunnableTest
				  (run-tests [this]
					     (list
					      (try-expectations this
								(expect= (/ 1 0) 0)))))))))

(def t6 (reify Testable
	       (get-tests [this]
			  (list
			   (reify RunnableTest
				  (run-tests [this]
					     (list
					      (try-expectations this
								(expect (< 1 0))))))))))