(ns lazytest.test_case)

(defn test-case [f]
  {:pre [(fn? f)]}
  (vary-meta f assoc ::test-case true))

(defn test-case? [x]
  (and (fn? x) (::test-case (meta x))))
