(ns foo-test
  (:use foo
	lazytest.describe))

(describe "The foo function"
  (it "says hello"
    (= "Hello, World!" (hello))))

(describe goodbye
  (it "says goodbye"
    (re-find #"Goodbye" (goodbye))))
