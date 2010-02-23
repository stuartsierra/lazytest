(ns #^{:doc "Assertions for lazytest.  Load this namespace to run the
    assertions; if it throws an Exception, there's a problem."
       :author "Stuart Sierra"}
  com.stuartsierra.lazytest-asserts
  (:use com.stuartsierra.lazytest))

;;; SELF-TESTS

;; Assertions
(def a1 (assertion [a] (pos? a)))

(assert (= :com.stuartsierra.lazytest/AssertionPassed (type (a1 1))))
(assert (= :com.stuartsierra.lazytest/AssertionFailed (type (a1 -1))))
(assert (= :com.stuartsierra.lazytest/AssertionThrown (type (a1 "string"))))

;; Contexts
(def c1 (Context nil nil nil))

(assert (= {} (open-context {} c1)))
(assert (= {} (close-context {} c1)))

(def c2 (Context nil (fn [] 2) (fn [x] (assert (= x 2)))))

(assert (= {c2 2} (open-context {} c2)))
(assert (= {} (close-context {c2 2} c2)))
(assert (= {c2 0} (open-context {c2 0} c2)))

(def c3 (Context [c2] (fn [s2] (assert (= s2 2)) 3)
                 (fn [x s2] (assert (= x 3)) (assert (= s2 2)))))

(assert (= {c2 2, c3 3} (open-context {} c3)))
(assert (= {} (close-context {c2 2, c3 3} c3)))

;; Simple TestCase
(def t1 (TestCase [c2] [a1 a1 a1]))

(assert (= :com.stuartsierra.lazytest/TestResult (type (run-test-case t1))))
(assert (every? #(= :com.stuartsierra.lazytest/AssertionPassed (type %))
                (:children (run-test-case t1))))

;; TestCase with nested Context
(def c4 (Context [c2] (fn [s2] (assert (= s2 2)) 4)
                 (fn [s4 s2] (assert (= s4 4)) (assert (= s2 2)))))

(def t2 (TestCase [c4] [a1]))

(assert (= :com.stuartsierra.lazytest/AssertionPassed
           (type (first (:children (run-test-case t2))))))

;; TestCase with multiple Contexts
(def a2 (assertion [x y] (< x y)))

(def t3 (TestCase [c2 c4] [a2 a2]))

(assert (every? #(= :com.stuartsierra.lazytest/AssertionPassed (type %))
                (:children (run-test-case t3))))

;; Context Ordering
(declare *log*)

(defn log [event]
  (swap! *log* conj event))

(defmacro with-log [& body]
  `(binding [*log* (atom [])]
     ~@body))

(def a3 (assertion [x] (log :a3) (pos? x)))

(def a4 (assertion [x] (log :a4) (pos? x)))

(def c5 (Context [] (fn [] (log :c5-open) 5)
                 (fn [x] (assert (= x 5)) (log :c5-close))))

(def t4 (TestCase [c5] [a3 a4]))

(with-log
  (run-test-case t4)
  (assert (= @*log* [:c5-open :a3 :a4 :c5-close])))

(def t5 (TestCase [] [t4 t4]))

(with-log
  (dorun (result-seq (run-test-case t5)))
  (assert (= @*log* [:c5-open :a3 :a4 :c5-close :c5-open :a3 :a4 :c5-close])))

(def t6 (TestCase [c5] [t4 t4]))

(with-log
  (dorun (result-seq (run-test-case t6 lazy-strategy)))
  (assert (= @*log* [:c5-open :a3 :a4 :a3 :a4 :c5-close])))

;; Lazy Evaluation
(def c6 (Context [] (fn [] (log :c6-open) 6) nil))

(def t7 (TestCase [c6] [a3 a4]))

(with-log
  (let [results (run-test-case t7 lazy-strategy)]
    (assert (= @*log* [:c6-open]))
    (dorun (result-seq results))
    (assert (= @*log* [:c6-open :a3 :a4]))))

;; Nested Lazy Evaluation
(def t8 (TestCase [] [t7 t7]))

(with-log
  (let [results (run-test-case t8 lazy-strategy)]
    (assert (= @*log* []))
    (dorun (:children (first (:children results))))
    (assert (= @*log* [:c6-open :a3 :a4]))
    (dorun (result-seq results))
    (assert (= @*log* [:c6-open :a3 :a4 :c6-open :a3 :a4]))))

(def t9 (TestCase [c6] [t7 t7]))

(with-log
  (let [results (run-test-case t9 lazy-strategy)]
    (assert (= @*log* [:c6-open]))
    (dorun (result-seq results))
    (assert (= @*log* [:c6-open :a3 :a4 :a3 :a4]))))

;; defassert form
(defassert a5 "Assertion a5" [a b]
  (comment "stuff")
  (< a b))

(assert (fn? a5))
(assert (= 'a5 (:name (meta a5))))
(assert (= *ns* (:ns (meta a5))))
(assert (= "Assertion a5" (:doc (meta a5))))
(assert (= :com.stuartsierra.lazytest/AssertionPassed (type (a5 1 2))))
(assert (= :com.stuartsierra.lazytest/AssertionFailed (type (a5 2 1))))
(assert (= :com.stuartsierra.lazytest/AssertionThrown (type (a5 "a" "b"))))

;; defcontext form
(defcontext c7 "Context c7" [s5 c5]
  (assert (= s5 5))
  7
  :after [s7]
  (assert (= s7 7)))
(assert (= :com.stuartsierra.lazytest/Context (type c7)))
(assert (= 'c7 (:name (meta c7))))
(assert (= *ns* (:ns (meta c7))))
(assert (= "Context c7" (:doc (meta c7))))
(assert (= [c5] (:parents c7)))
(assert (fn? (:before c7)))
(assert (fn? (:after c7)))

;; deftest form
(deftest t10 "Test t10"
  [a c4, b c5]
  (= a b)
  "a is less than b"
  (< a b))

(assert (= 't10 (:name (meta t10))))
(assert (= *ns* (:ns (meta t10))))
(assert (= "Test t10" (:doc (meta t10))))
(assert (= :com.stuartsierra.lazytest/TestCase (type t10)))
(assert (= [c4 c5] (:contexts t10)))
(assert (every? fn? (:children t10)))
(assert (= "a is less than b"
           (:doc (meta (second (:children t10))))))
(assert (= '(= a b)
           (:form (meta (first (:children t10))))))

;; defsuite form
(defsuite t11 "Suite t11" [c4 c5] t9 t10)

(assert (= 't11 (:name (meta t11))))
(assert (= *ns* (:ns (meta t11))))
(assert (= "Suite t11" (:doc (meta t11))))
(assert (= :com.stuartsierra.lazytest/TestCase (type t11)))
(assert (= [c4 c5] (:contexts t11)))
(assert (= [t9 t10] (:children t11)))


;; README examples
    (defassert positive [x] (pos? x))
    (success? (positive 1))
    ;;=> true
    (success? (positive -1))
    ;;=> false
    (success? (positive "hello"))
    ;;=> false
    (deftest addition [a 1, b 2]
      (integer? a)
      (integer? b)
      (integer? (+ a b)))
    (success? (addition))
    ;;=> true
    (defcontext random-int []
      (rand-int Integer/MAX_VALUE))
    (deftest random-addition [a random-int, b random-int]
      (integer? (+ a b))
      (= (+ a b) (+ b a)))

    (success? (random-addition))

    (deftest failure "This test always fails." [] (= 1 0))

    (defsuite all-tests [] addition random-addition failure)

    (success? (all-tests))
    ;;=> false
