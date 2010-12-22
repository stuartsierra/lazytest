(ns lazytest.nsdeps-asserts
  (:use lazytest.nsdeps))

(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use a.b)))))
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use [a.b])))))
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use (a b))))))
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use [a.b :only (c)])))))
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use (a [b :only (c)]))))))
(assert (= #{'a.b 'a.c 'a.d} (deps-from-ns-decl '(ns x (:use (a b c d))))))

;; nested use (penumbra uses libspecs like this)
(assert (= #{'a.b 'a.c} (deps-from-ns-decl '(ns x (:use (a [b :only (d)] [c :only (e)]))))))

;; Keyword flags in :use (ugh, but 'lein new' does it by default)
(assert (= #{'a.b} (deps-from-ns-decl '(ns x (:use [a.b] :reload)))))
