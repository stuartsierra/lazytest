(ns examples.describe1
  (:use lazytest.describe
	[lazytest.context :only (fn-context)]
	[lazytest.context.stateful :only (stateful-fn-context)]))

(describe +
  (it "computes the sum of 3 and 4"
    (= 7 (+ 3 4))))

(describe + "given any 2 integers"
  (for [x (range 5), y (range 5)]
    (it "is commutative"
      (= (+ x y) (+ y x)))))

(describe +
  (testing "with integers"
    (it "computes sums of small numbers"
      (= 7 (+ 3 4)))
    (it "computes sums of large numbers"
      (= 7000000 (+ 3000000 4000000))))
  (testing "with floating point"
    (it "computes sums of small numbers"
      (= 0.0000007 (+ 0.0000003 0.0000004)))
    (it "computes sums of large numbers"
      (= 7000000.0 (+ 3000000.0 4000000.0)))))

(describe "The do-it macro"
  (do-it "allows arbitrary code"
	 (println "Hello, do-it!")
	 (println "This test will succeed because it doesn't throw.")))

(describe "The square root of two"
  (given [root (Math/sqrt 2)]
	 (it "is less than two"
	   (< root 2))
	 (it "is more than one"
	   (> root 1))))

(describe "Addition with a context"
  (with [(fn-context #(println "This happens before each test example")
		     #(println "This happens after each test example"))]
    (it "adds small numbers"
      (= 7 (+ 3 4)))
    (it "adds large numbers"
      (= 7000 (+ 3000 4000)))))

(describe "Addition with a context"
  (with [(fn-context #(println "This happens before all tests")
		     #(println "This happens after all tests"))]
    (testing "with a nested group"
      (it "adds small numbers"
	(= 7 (+ 3 4)))
      (it "adds large numbers"
	(= 7000 (+ 3000 4000))))))

(describe "Square root of two with state"
  (using [root (stateful-fn-context
		  (fn [] (Math/sqrt 2))
		  (fn [x] (println "All done with" x)))]
    (it "is less than 2"
      (> 2 @root))
    (it "is more than 1"
      (< 1 @root))))

