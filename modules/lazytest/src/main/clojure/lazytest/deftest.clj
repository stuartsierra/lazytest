(ns lazytest.deftest
  "A drop-in replacement for clojure.test.  Supports the 'deftest',
  'is', 'are', and 'testing' macros.  Currently 'testing' doc strings
  do not appear in the report output."
  (:use lazytest.expect
	lazytest.test-case
	[lazytest.expect.thrown :only (throws? throws-with-msg?)])
  (:require clojure.template))

(def ^:dynamic *testing* nil)

(defmacro testing [doc & body]
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  `(binding [*testing* (cons ~doc *testing*)]
     ~@body))

(defmacro thrown? [c expr]
  `(throws? ~c (fn [] ~expr)))

(defmacro thrown-with-msg? [c re expr]
  `(throws-with-msg? ~c ~re (fn [] ~expr)))

(defmacro is
  "Generic assertion macro.  'form' is any predicate test.
  'msg' is an optional message to attach to the assertion.
  
  Example: (is (= 4 (+ 2 2)) \"Two plus two should be 4\")

  Special forms:

  (is (thrown? c body)) checks that an instance of c is thrown from
  body, fails if not; then returns the thing thrown.

  (is (thrown-with-msg? c re body)) checks that an instance of c is
  thrown AND that the message on the exception matches (with
  re-find) the regular expression re."
  ([expr]
     `(expect nil ~expr))
  ([expr message]
     `(expect ~message ~expr)))

(defmacro are [argv expr & args]
  "Checks multiple assertions with a template expression.
  See clojure.template/do-template for an explanation of
  templates.

  Example: (are [x y] (= x y)  
                2 (+ 1 1)
                4 (* 2 2))
  Expands to: 
           (do (is (= 2 (+ 1 1)))
               (is (= 4 (* 2 2))))

  Note: This breaks some reporting features, such as line numbers."
  `(clojure.template/do-template ~argv (is ~expr) ~@args))

(defmacro deftest
  "Defines a test function with no arguments."
  [sym & body]  
  {:pre [(symbol? sym)]}
  `(def ~sym (vary-meta (test-case (fn [] ~@body))
			assoc :name '~sym)))
