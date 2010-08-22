(ns examples.describe1
  (:use lazytest.describe))

(describe +
  (it "computes the sum of 3 and 4"
    (= 7 (+ 3 4))))

(describe + "given any 2 integers"
  (for [x (range 5), y (range 5)]
    (it "is commutative"
      (= (+ x y) (+ y x)))))
