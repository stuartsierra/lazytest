(ns examples.difference
  (:use lazytest.describe))

(describe "Differences among"
  (it "simple values"
    (= 1 2))
  (it "simple computed values"
    (= 5 (+ 2 2)))
  (it "simple strings with common prefix"
    (= "foobar" "fooquux"))
  (it "vectors"
    (= [1 2 3 4] [1 2 :a 4]))
  (it "vectors of different lengths"
    (= [1 2 3] [1]))
  (it "vectors of different lengths"
    (= [1] [1 1 3]))
  (it "maps with similar keys"
    (= {:a 1} {:a 2}))
  (it "maps with different keys"
    (= {:a 1} {:b 2}))
  (it "sets"
    (= #{1 2 3} #{2 3 4})))