(ns lazytest.nsdeps-asserts
  (:use lazytest.nsdeps))

(defn deps-from-ns-decl-test [& args]
  (let [test-cases (partition 2 args)]
    (doseq [case test-cases]
      (let [deps (deps-from-ns-decl (first case))]
        (assert (= deps (last case)))))))

(deps-from-ns-decl-test
 '(ns x (:use a.b))               #{'a.b}
 '(ns x (:use [a.b]))             #{'a.b}
 '(ns x (:use (a b)))             #{'a.b}
 '(ns x (:use [a.b :only (c)]))   #{'a.b}
 '(ns x (:use (a (b :only (c))))) #{'a.b}
 '(ns x (:use (a [b :only (c)]))) #{'a.b}
 '(ns x (:use (a b c d)))         #{'a.b 'a.c 'a.d}
 '(ns x (:use (a (b c d))))       #{'a.b.c 'a.b.d})
