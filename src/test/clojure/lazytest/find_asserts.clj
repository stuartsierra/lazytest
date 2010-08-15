(ns lazytest.find-asserts
  (:use lazytest.find))

;;; find-tests

;; returns empty for namespaces with no tests
(assert (empty? (find-tests (create-ns 'one))))
(assert (empty? (find-tests (intern 'one 'a))))
(assert (empty? (find-tests (the-ns 'one))))
(assert (empty? (find-tests (intern 'one 'b "hello"))))
(remove-ns 'one)

;; recurses on FindTests Vars
(create-ns 'two)
(intern 'two 'a (reify FindTests (find-tests [this] (list :a))))
(assert (= (list :a) (find-tests (the-ns 'two))))
(remove-ns 'two)

;; maps over the sequence of (all-ns)
(create-ns 'three)
(intern 'three 'b (reify FindTests (find-tests [this] (list :b))))
(create-ns 'four)
(intern 'four 'c (reify FindTests (find-tests [this] (list :c))))
(assert (= #{:b :c} (set (mapcat find-tests (list (the-ns 'three) (the-ns 'four))))))
(remove-ns 'three)
(remove-ns 'four)

;; filters :focused tests
(create-ns 'five)
(intern 'five 'd (reify FindTests (find-tests [this] (list 'd))))
(intern 'five 'e (reify FindTests (find-tests [this] (list (with-meta 'e
							   {:focused true})))))
(assert (= (list 'e) (find-tests (the-ns 'five))))
(remove-ns 'five)
