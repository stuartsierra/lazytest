(ns lazytest.describe-asserts
  (:use	[lazytest.find :only (find-tests)]
	[lazytest.runnable :only (run-tests)]
	[lazytest.result :only (success?)]))


;; Passing test
(remove-ns 'one)
(ns one (:use lazytest.describe))
(describe "Addition"
  (it "adds"
    (= 4 (+ 2 2))))

(in-ns 'lazytest.describe-asserts)
(let [result (first (mapcat run-tests (find-tests (the-ns 'one))))]
  (assert (success? result)))
(remove-ns 'one)


;; Failing test
(remove-ns 'two)
(ns two (:use lazytest.describe))
(describe "Addition"
  (it "adds"
    (= 999 (+ 2 2))))

(in-ns 'lazytest.describe-asserts)
(let [results (first (mapcat run-tests (find-tests (the-ns 'two))))]
  (assert (not (success? results)))
  (let [one-result (first (:children results))]
    (assert (instance? lazytest.result.Fail one-result))
    (assert (= (list = 999 4) (:evaluated (:reason one-result))))))
(remove-ns 'two)


;; Single given
(remove-ns 'three)
(ns three (:use lazytest.describe))
(describe "Addition"
  (given [seven 7]
	 (it "adds"
	   (= seven (+ 3 4)))))

(in-ns 'lazytest.describe-asserts)
(let [result (first (mapcat run-tests (find-tests (the-ns 'three))))]
  (assert (success? result)))
(remove-ns 'three)


;; Empty tests are marked pending, count as success
(remove-ns 'four)
(ns four (:use lazytest.describe))
(describe "Addition"
  (it "does something"))

(in-ns 'lazytest.describe-asserts)
(let [results (first (mapcat run-tests (find-tests (the-ns 'four))))]
  (assert (success? results))
  (let [one-result (first (:children results))]
    (assert (instance? lazytest.result.Pending one-result))))
(remove-ns 'four)


;; Skipped tests count as success
(remove-ns 'five)
(ns five (:use lazytest.describe))
(describe "Addition"
  (it "does something"
    {:skip true}
    (= 99 10)))

(in-ns 'lazytest.describe-asserts)
(let [results (first (mapcat run-tests (find-tests (the-ns 'five))))]
  (assert (success? results))
  (let [one-result (first (:children results))]
    (assert (instance? lazytest.result.Skip one-result))))
(remove-ns 'five)


;; 'given' blocks can be nested
(remove-ns 'six)
(ns six (:use lazytest.describe))
(describe "Numbers"
  (given [x 1]
	 (given [y 1]
		(it "are equal"
		  (= x y)))))

(in-ns 'lazytest.describe-asserts)
(let [results (first (mapcat run-tests (find-tests (the-ns 'six))))]
  (assert (success? results)))
(remove-ns 'six)


;; 'given' blocks can contain multiple bindings
(remove-ns 'seven)
(ns seven (:use lazytest.describe))
(describe "Numbers"
  (given [x 1
	  y 1]
	 (it "are equal"
	   (= x y))))

(in-ns 'lazytest.describe-asserts)
(let [results (first (mapcat run-tests (find-tests (the-ns 'seven))))]
  (assert (success? results)))
(remove-ns 'seven)
