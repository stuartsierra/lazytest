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
	(with-meta
	  (common-test-cases 1)
	  {:doc "One"}))))

(def s2
     (suite
      (fn []
	(after
	 (before
	  (with-meta
	    (common-test-cases 2)
	    {:doc "Two"})
	  #(prn "Before Two"))
	 #(prn "After Two")))))

(def s3
     (suite
      (fn []
	(with-meta
	 (map (fn [tc]
		(after (before tc #(prn "Before test case"))
		       #(prn "After test case")))
	      (common-test-cases 3))
	 {:doc "Three"}))))