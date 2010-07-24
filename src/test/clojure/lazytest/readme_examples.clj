(ns lazytest.readme-examples
    (:use [lazytest.expect :only (expect thrown?)]
	  [lazytest.describe :only (describe it given)]))

    (expect (= 3 (+ 1 2)))
    (expect (thrown? ArithmeticException (/ 5 0)))

    (describe + "with integers"
      (it "computes the sum"
        (expect (= 3 (+ 1 2))
	        (= 7 (+ 3 4)))))

    (given "The square root of 2" [s (Math/sqrt 2)]
      (it "is less than 2"
        (expect (< s 2)))
      (it "is greater than 1"
        (expect (> s 1))))
