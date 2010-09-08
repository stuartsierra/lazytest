(ns examples.suite1
  (:use lazytest.suite
	lazytest.test-case
	lazytest.expect
	lazytest.context))

(defn common-test-cases [x]
  (list
   (vary-meta
    (test-case #(expect (= x 1)))
    assoc :doc "x equals one")
   (vary-meta
    (test-case #(expect (= x 2)))
    assoc :doc "x equals two")))

(def s1
     (suite
      (fn []
	(vary-meta
	 (test-seq (common-test-cases 1))
	 assoc :doc "One"))))

(def s2
     (suite
      (fn []
	(add-context
	 (vary-meta
	  (test-seq (common-test-cases 2))
	  assoc :doc "Two")
	 (fn-context #(prn "Before Two") #(prn "After Two"))))))

(def s3
     (suite
      (fn []
	(vary-meta
	  (test-seq (map (fn [tc]
			   (add-context tc (fn-context #(prn "Before test case")
						       #(prn "After test case"))))
			 (common-test-cases 3)))
	  assoc :doc "Three"))))

(def s4
     (vary-meta
      (suite
       (fn []
	 (test-seq (common-test-cases 4))))
      assoc :doc "Four"))