(ns examples.describe1
  (:use lazytest.describe))

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
