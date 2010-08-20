(ns lazytest.case)

(defn test-case [f]
  (vary-meta f assoc ::case true))

(defn test-case? [x]
  (and (fn? x) (::case (meta x))))

