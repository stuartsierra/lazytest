(ns lazytest.testable-asserts
  (:use lazytest.testable))

;;; get-tests

;; returns empty for namespaces with no tests
(assert (empty? (get-tests [])))
(assert (empty? (get-tests (create-ns 'one))))
(assert (empty? (get-tests (intern 'one 'a))))
(assert (empty? (get-tests (the-ns 'one))))
(assert (empty? (get-tests (intern 'one 'b "hello"))))
(remove-ns 'one)

;; recurses on Testable Vars
(create-ns 'two)
(intern 'two 'a (reify Testable (get-tests [this] (list :a))))
(assert (= (list :a) (get-tests (the-ns 'two))))
(remove-ns 'two)

;; recurses on the sequence of (all-ns)
(create-ns 'three)
(intern 'three 'b (reify Testable (get-tests [this] (list :b))))
(create-ns 'four)
(intern 'four 'c (reify Testable (get-tests [this] (list :c))))
(assert (= #{:b :c} (set (get-tests (list (the-ns 'three) (the-ns 'four))))))
(remove-ns 'three)
(remove-ns 'four)

;; filters :focused tests
(create-ns 'five)
(intern 'five 'd (reify Testable (get-tests [this] (list 'd))))
(intern 'five 'e (reify Testable (get-tests [this] (list (with-meta 'e
							   {:focused true})))))
(assert (= (list 'e) (get-tests (the-ns 'five))))
(remove-ns 'five)
