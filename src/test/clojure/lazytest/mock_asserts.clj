(ns lazytest.mock-asserts
  (:use [lazytest.fixture :only (setup teardown)]
	lazytest.mock))

(defn foo [] 0)

(def mock-1 (mock #'foo (constantly 1)))
(def mock-2 (mock #'foo (constantly 2)))

(let [] ;; need lexical context for push/pop bindings
  (assert (= 0 (foo)))
  (setup mock-1)
  (try
    (assert (= 1 (foo)))
    (setup mock-2)
    (try
      (assert (= 2 (foo)))
      (finally (teardown mock-2)))
    (assert (= 1 (foo)))
    (finally (teardown mock-1)))
  (assert (= 0 (foo))))


(def gmock-1 (global-mock #'foo (constantly 1)))
(def gmock-2 (global-mock #'foo (constantly 2)))

(let [] ;; need lexical context for push/pop bindings
  (assert (= 0 (foo)))
  (setup gmock-1)
  (try
    (assert (= 1 (foo)))
    (setup gmock-2)
    (try
      (assert (= 2 (foo)))
      (finally (teardown gmock-2)))
    (assert (= 1 (foo)))
    (finally (teardown gmock-1)))
  (assert (= 0 (foo))))

