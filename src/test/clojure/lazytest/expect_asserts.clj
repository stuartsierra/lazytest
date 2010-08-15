(ns lazytest.expect-asserts
  (:use lazytest.expect
	lazytest.expect.thrown)
  (import (lazytest ExpectationFailed)))

(expect (= 1 1))
(expect (not= 1 2))
(expect (instance? java.lang.String "Hello, World!"))

(expect (thrown? Exception (do (throw (IllegalArgumentException.)))))
(expect (thrown-with-msg? Exception #"foo message"
	  (do (throw (Exception. "the foo message for this exception")))))

(expect (caused? IllegalArgumentException
		 (do (throw (IllegalArgumentException. "bad arguments")))))

(expect (caused? IllegalArgumentException
		 (do (try
		       (throw (IllegalArgumentException. "bad stuff"))
		       (catch IllegalArgumentException e
			 (throw (RuntimeException. "wrapped stuff" e)))))))

(let [e1 (try (expect (= 1 2))
	      false
	      (catch ExpectationFailed err err))]
  (assert e1)
  (let [reason (.reason e1)]
    (assert (= '(= 1 2) (:form reason)))
    (assert (= (list = 1 2) (:evaluated reason)))
    (assert (false? (:result reason)))))

(let [e3 (try (expect (instance? java.lang.String 42))
	      false
	      (catch ExpectationFailed err err))]
  (assert e3)
  (let [reason (.reason e3)]
    (assert (= '(instance? java.lang.String 42) (:form reason)))
    (assert (= (list instance? java.lang.String 42) (:evaluated reason)))
    (assert (false? (:result reason)))))
