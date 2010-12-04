(ns examples.readme)

    (ns examples.readme.groups
      (:use [lazytest.describe :only (describe it)]))

    (describe + "with integers"
      (it "computes the sum of 1 and 2"
        (= 3 (+ 1 2)))
      (it "computes the sum of 3 and 4"
        (= 7 (+ 3 4))))


    (ns examples.readme.nested
      (:use [lazytest.describe :only (describe it testing)]))

    (describe "Addition"
      (testing "of integers"
        (it "computes small sums"
          (= 3 (+ 1 2)))
        (it "computes large sums"
          (= 7000 (+ 3000 4000))))
      (testing "of floats"
        (it "computes small sums"
          (> 0.00001 (Math/abs (- 0.3 (+ 0.1 0.2)))))
        (it "computes large sums"
          (> 0.00001 (Math/abs (- 3000.0 (+ 1000.0 2000.0)))))))


    (ns examples.readme.givens
      (:use [lazytest.describe :only (describe it given)]))

    (describe "The square root of two"
      (given [root (Math/sqrt 2)]
        (it "is less than two"
          (< root 2))
        (it "is more than one"
          (> root 1))))


    (ns examples.readme.do-it
      (:use [lazytest.describe :only (describe do-it)]
            [lazytest.expect :only (expect)]))

    (describe "Arithmetic"
      (do-it "after printing"
        (expect (= 4 (+ 2 2)))
        (println "Hello, World!")
        (expect (= -1 (- 4 5)))))
