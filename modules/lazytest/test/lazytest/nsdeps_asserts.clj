(ns lazytest.nsdeps-asserts
  (:use lazytest.nsdeps))

(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use a.b)))))
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use [a.b])))))
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use (a b))))))
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use [a.b :only (c)])))))
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use (a [b :only (c)]))))))
(assert (= #{'a.b 'a.c 'a.d} (deps-from-ns-decl '(ns x (:use (a b c d))))))

;; Nested prefix lists (ugh)
(assert (= #{'a.b.c 'a.b.d} (deps-from-ns-decl '(ns x (:use (a (b c d)))))))

;; Keyword flags in :use (ugh, but 'lein new' does it by default)
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use [a.b] :reload)))))
