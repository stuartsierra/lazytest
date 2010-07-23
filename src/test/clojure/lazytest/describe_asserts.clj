(ns lazytest.describe-asserts
  (:use	[lazytest.testable :only (get-tests)]
	[lazytest.runnable-test :only (run-tests)]
	[lazytest.test-result :only (success?)]))

(remove-ns 'one)
(ns one (:use lazytest.describe lazytest.expect))
(describe "Addition"
  (it "adds"
    (expect (= 4 (+ 2 2)))))

(in-ns 'lazytest.describe-asserts)
(let [result (first (mapcat run-tests (get-tests (the-ns 'one))))]
  (assert (success? result)))
(remove-ns 'one)


(remove-ns 'two)
(ns two (:use lazytest.describe lazytest.expect))
(describe "Addition"
  (it "adds"
    (expect (= 999 (+ 2 2)))))

(in-ns 'lazytest.describe-asserts)
(let [result (first (mapcat run-tests (get-tests (the-ns 'two))))]
  (assert (not (success? result))))
(remove-ns 'two)


(remove-ns 'three)
(ns three (:use lazytest.describe lazytest.expect))
(describe "Addition"
  (given [seven 7]
	 (it "adds"
	   (expect (= seven (+ 3 4))))))

(in-ns 'lazytest.describe-asserts)
(let [result (first (mapcat run-tests (get-tests (the-ns 'three))))]
  (assert (success? result)))
(remove-ns 'three)
