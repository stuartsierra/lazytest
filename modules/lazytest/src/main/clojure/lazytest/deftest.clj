(ns lazytest.deftest
  (:use lazytest.expect
	lazytest.test-case))

(def ^:dynamic *testing* nil)

(defmacro testing [doc & body]
  `(binding [*testing* (cons ~doc *testing*)]
     ~@body))

(defmacro is
  ([expr]
     `(expect nil ~expr))
  ([expr message]
     `(expect ~message ~expr)))

(defmacro deftest [sym & body]
  {:pre [(symbol? sym)]}
  `(def ~sym (vary-meta (test-case (fn [] ~@body))
			assoc :name '~sym)))
