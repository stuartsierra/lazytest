(ns math-tests
  (:use lazytest.describe))

(describe + "with integers"
  (it "should add small numbers"
    (= 7 (+ 3 4)))
  (it "should add large numbers"
    (= 53924864 (+ 41885013 12039851)))
  (it "should add negative numbers"
    (= -10 (+ -4 -6))))

(describe + "with floating-point numbers"
  (it "should add small numbers"
    (= 3.5 (+ 2.0 1.5)))
  (it "should add negative numbers"
    (= -3.5 (+ -2.0 -1.5))))
