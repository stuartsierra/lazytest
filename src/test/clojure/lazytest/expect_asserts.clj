(ns lazytest.expect-asserts
  (:use lazytest.expect)
  (import (lazytest ExpectationFailed)))

(expect (= 1 1))
(expect (not= 1 2))
(expect (instance? java.lang.String "Hello, World!"))
(expect-thrown Exception (do (throw (IllegalArgumentException.))))
(expect-thrown-with-msg Exception #"foo message"
  (do (throw (Exception. "the foo message for this exception"))))


(let [e1 (try (expect (= 1 2))
	      false
	      (catch ExpectationFailed err err))]
  (assert e1)
  (let [reason (.reason e1)]
    (assert (instance? lazytest.failure.NotEqual reason))
    (assert (= (list 1 2) (:objects reason)))))

(let [e2 (try (expect (not= 1 1))
	      false
	      (catch ExpectationFailed err err))]
  (assert e2)
  (let [reason (.reason e2)]
    (assert (instance? lazytest.failure.NotNotEqual reason))
    (assert (= (list 1 1) (:objects reason)))))

(let [e3 (try (expect (instance? java.lang.String 42))
	      false
	      (catch ExpectationFailed err err))]
  (assert e3)
  (let [reason (.reason e3)]
    (assert (instance? lazytest.failure.NotInstanceOf reason))
    (assert (= java.lang.String (:expected-class reason)))
    (assert (= java.lang.Integer (:actual-class reason)))))

(let [e4 (try (expect-thrown IllegalArgumentException nil)
	      false
	      (catch ExpectationFailed err err))]
  (assert e4)
  (let [reason (.reason e4)]
    (assert (instance? lazytest.failure.NotThrown reason))
    (assert (= IllegalArgumentException (:class reason)))))

(let [e5 (try (expect-thrown-with-msg Exception #"message foo"
			     (do (throw (Exception. "this is not foo"))))
	      false
	      (catch ExpectationFailed err err))]
  (assert e5)
  (let [reason (.reason e5)]
    (assert (instance? lazytest.failure.ThrownWithWrongMessage reason))
    (assert (instance? java.util.regex.Pattern (:expected-re reason)))
    (assert (= "this is not foo" (:actual-message reason)))))
