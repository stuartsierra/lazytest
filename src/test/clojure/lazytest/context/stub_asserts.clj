(ns lazytest.context.stub-asserts
  (:use [lazytest.context :only (setup teardown)]
	lazytest.context.stub))

(defn foo [] 0)

(def stub-1 (stub #'foo (constantly 1)))
(def stub-2 (stub #'foo (constantly 2)))

(let [] ;; need lexical context for push/pop bindings
  (assert (= 0 (foo)))
  (setup stub-1)
  (try
    (assert (= 1 (foo)))
    (setup stub-2)
    (try
      (assert (= 2 (foo)))
      (finally (teardown stub-2)))
    (assert (= 1 (foo)))
    (finally (teardown stub-1)))
  (assert (= 0 (foo))))


(def gstub-1 (global-stub #'foo (constantly 1)))
(def gstub-2 (global-stub #'foo (constantly 2)))

(let [] ;; need lexical context for push/pop bindings
  (assert (= 0 (foo)))
  (setup gstub-1)
  (try
    (assert (= 1 (foo)))
    (setup gstub-2)
    (try
      (assert (= 2 (foo)))
      (finally (teardown gstub-2)))
    (assert (= 1 (foo)))
    (finally (teardown gstub-1)))
  (assert (= 0 (foo))))

