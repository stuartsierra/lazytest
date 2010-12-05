(ns lazytest.deftest
  "A drop-in replacement for clojure.test.  Supports the 'deftest',
  'is', 'are', and 'testing' macros.  Currently 'testing' doc strings
  do not appear in the report output."
  (:use [clojure.string :only (join)]
	lazytest.expect
	lazytest.test-case
	[lazytest.expect.thrown :only (throws? throws-with-msg?)])
  (:require clojure.template))

(def ^:dynamic *testing* nil)

(defmacro testing [doc & body]
  "Adds a new string to the list of testing contexts.  May be nested,
  but must occur inside a test function (deftest)."
  `(binding [*testing* (cons ~doc *testing*)]
     ~@body))

(defn current-testing-string
  "Returns the result of concatenating the sequence of nested
  documentation strings created with the `testing` macro."
  []
  (when (seq *testing*)
    (join " " (reverse *testing*))))

(defmacro thrown?
  "Returns true if expr throws an exception of class c."
  [c expr]
  `(throws? ~c (fn [] ~expr)))

(defmacro thrown-with-msg?
  "Returns true if expr throws an exception of class c with a message
  matching regular expression re."
  [c re expr]
  `(throws-with-msg? ~c ~re (fn [] ~expr)))

(defmacro is
  "Generic assertion macro.  'form' is any predicate test.
  'msg' is an optional message to attach to the assertion.
  
  Example: (is (= 4 (+ 2 2)) \"Two plus two should be 4\")"
  ([expr]
     `(expect (current-testing-string) ~expr))
  ([expr message]
     `(testing ~message
	(expect (current-testing-string) ~expr))))

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
			merge
			'~(meta sym)
			{:name '~sym})))
