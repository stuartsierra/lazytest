(ns lazytest.describe-asserts
  (:use lazytest.describe
	[lazytest.expect :only (expect)]
	[lazytest.testable :only (get-tests)]
	[lazytest.runnable-test :only (run-tests)]
	[lazytest.test-result :only (success?)]))

(remove-ns 'one)
(create-ns 'one)
(intern 'one 'a (describe "Addition"
		  (it "adds"
		    (expect (= 4 (+ 2 2))))))
(let [result (first (map run-tests (get-tests (the-ns 'one))))]
  (assert (success? result)))
(remove-ns 'one)

(remove-ns 'two)
(create-ns 'two)
(intern 'two 'b (describe "Addition"
		  (it "adds"
		    (expect (= 999 (+ 2 2))))))
(let [result (first (map run-tests (get-tests (the-ns 'two))))]
  (assert (not (success? result))))
(remove-ns 'two)
