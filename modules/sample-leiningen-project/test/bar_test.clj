(ns bar-test
  (:use bar
	lazytest.describe))

(describe "The bar namespace"
  (it "returns foo"
    (re-matches #".*foo.*" (foo))))