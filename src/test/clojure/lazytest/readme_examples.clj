(ns lazytest.readme-examples
    (:use [lazytest.expect :only (expect thrown?)]
	  [lazytest.describe :only (describe it do-it given)]))

    (describe + "with integers"
      (it "computes the sum of 1 and 2"
        (= 3 (+ 1 2)))
      (it "computes the sume of 3 and 4"
	(= 7 (+ 3 4))))

    (describe "Arithmetic"
      (do-it "after printing"
        (println "Hello, World!")
        (expect (= 4 (+ 2 2)))))

    (describe
      (given "The square root of 2" [s (Math/sqrt 2)]
        (it "is less than 2"
          (< s 2))
        (it "is greater than 1"
          (> s 1))))
