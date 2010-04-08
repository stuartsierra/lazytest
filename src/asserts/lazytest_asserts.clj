(ns lazytest-asserts
  (:use com.stuartsierra.lazytest
        com.stuartsierra.lazytest.report))

;;; Basics

(spec simple-asserts
      (is (= 2 (+ 1 1))
          (= 4 (+ 2 2))
          (empty? [])))

(spec simple-failures
      (is (= 3 (+ 1 1))
          (= 5 (+ 2 2))
          (empty? "foo")))

(assert (success? (simple-asserts)))
(doseq [c (result-seq (simple-asserts))]
  (assert (success? c)))

(assert (not (success? (simple-failures))))
(doseq [c (result-seq (simple-failures))]
  (assert (not (success? c))))

(spec simple-suite simple-asserts simple-failures)

(assert (not (success? (simple-suite))))
(assert (success? (first (:children (simple-suite)))))
(assert (not (success? (second (:children (simple-suite))))))

;;; Pending tests

(spec pending-spec)

(assert (pending? (pending-spec)))

;;; Nested definitions

(spec top-level
      (spec first-child
            (is (= 1 1)))
      (spec second-child
            (is (= 2 1))))

(assert (not (success? (top-level))))
(assert (success? (first (:children (top-level)))))
(assert (success? (first-child)))
(assert (not (success? (second-child))))

;;; RSpec-style definitions

(spec psycho "Psychotic addition"
      (spec "with positive numbers"
            (spec "should work for"
                  (is "three"
                      (= 3 (+ 1 1))
                      "five"
                      (= 5 (+ 2 2)))))
      (spec "with negative numbers"
            (spec "should work for"
                  (is "three"
                      (= -3 (+ -1 -1))
                      "five"
                      (= -5 (+ -2 -2))))))

;;; Contexts

(declare *log*)

(defcontext c1 []
  (swap! *log* conj :open-c1)
  1
  :after [s]
  (assert (= s 1))
  (swap! *log* conj :close-c1))

(defcontext c2 []
  (swap! *log* conj :open-c2)
  2
  :after [s]
  (assert (= s 2))
  (swap! *log* conj :close-c2))

(defmacro with-log [& body]
  `(binding [*log* (atom [])]
     ~@body))

(spec repeat-contexts
      (given [a c1, b c2]
             (is (= a 1)
                 (= b 2))))

(with-log
  (let [result (repeat-contexts)]
    (dorun (result-seq result))
    (assert (success? result))
    (assert (= @*log* 
               [:open-c1 :open-c2 :close-c2 :close-c1
                :open-c1 :open-c2 :close-c2 :close-c1]))))

(spec once-contexts :contexts [c1 c2]
      (given [a c1, b c2]
             (is (= a 1)
                 (= b 2))))

(with-log
  (let [result (once-contexts)]
    (assert (success? result))
    (assert (= @*log* [:open-c1 :open-c2 :close-c2 :close-c1]))))


;; empty describe to block run-spec
(describe *ns* )
