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


    (ns examples.readme.contexts
     (:use [lazytest.describe :only (describe testing it with)]
           [lazytest.context :only (fn-context)]))

    (def my-context
      (fn-context (fn [] (println "This happens during setup"))
                  (fn [] (println "This happens during teardown"))))

    (describe "Addition with a context"
      (with [my-context]
        (it "adds small numbers"
          (= 7 (+ 3 4)))
        (it "adds large numbers"
          (= 7000 (+ 3000 4000)))))

    (describe "Addition with a context"
      (with [my-context]
        (testing "with a nested group"
          (it "adds small numbers"
            (= 7 (+ 3 4)))
          (it "adds large numbers"
            (= 7000 (+ 3000 4000))))))


    (ns examples.readme.before-after
      (use [lazytest.describe :only (describe it with before after)]))

    (describe "Addition with a context"
      (with [(before (println "This happens before each test"))
             (after (println "This happens after each test"))]
        (it "adds small numbers"
          (= 7 (+ 3 4)))
        (it "adds large numbers"
          (= 7000 (+ 3000 4000)))))


    (ns examples.readme.stateful-contexts
      (:use [lazytest.context.stateful :only (stateful-fn-context)]
            [lazytest.describe :only (describe using it)]))

    (describe "Square root of two with state"
      (using [root (stateful-fn-context
                     (fn [] (Math/sqrt 2))
                     (fn [x] (println "All done with" x)))]
        (it "is less than 2"
          (> 2 @root))
        (it "is more than 1"
          (< 1 @root))))
