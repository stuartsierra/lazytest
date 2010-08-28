(ns examples.suite1
  (:use lazytest.suite
	lazytest.test-case
	lazytest.expect
	lazytest.wrap))

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
	(after
	 (before
	  (vary-meta
	    (test-seq (common-test-cases 2))
	    assoc :doc "Two")
	  #(prn "Before Two"))
	 #(prn "After Two")))))

(def s3
     (suite
      (fn []
	(vary-meta
	  (test-seq (map (fn [tc]
			   (after (before tc #(prn "Before test case"))
				  #(prn "After test case")))
			 (common-test-cases 3)))
	  assoc :doc "Three"))))