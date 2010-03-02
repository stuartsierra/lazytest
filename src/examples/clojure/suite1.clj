(ns suite1
  (:use [com.stuartsierra.lazytest :only (deftest defsuite)]))

(deftest even-odd []
  (odd? 1)
  (even? 2)
  (odd? 3)
  (even? 4))

(deftest zero "Zero is even, not odd" []
  (even? 0)
  "Zero is not odd"
  (odd? 0))

(defsuite numbers "Simple number tests" []
  even-odd zero)

(deftest addition []
  (= 2 (+ 1 1))
  (= 3 (+ 2 1))
  "Psychotic math"
  (= 5 (+ 2 2)))

(deftest subtraction []
  (= 0 (- 1 1))
  (= 1 (- 2 1))
  (= 3 (- 5 2)))

(defsuite arithmetic []
  addition subtraction)

(defsuite all-tests []
  numbers arithmetic)