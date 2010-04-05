(ns #^{:doc "Compatibility layer for clojure.test

  :use this namespace instead of clojure.test, and your tests will be
  runnable with lazytest.  Failures in clojure.test will be reported
  as AssertionError by lazytest.

  Does not support clojure.test fixtures."}
  com.stuartsierra.lazytest.compat
  (:require clojure.test
            [com.stuartsierra.lazytest :as t]))

(defmacro deftest [name & body]
  `(t/spec-do ~name [] ~@body))

(defmacro testing [string & body]
  `(do ~@body))

(defmacro is [expr & msg]
  `(assert ~expr))

(defmacro are [argv expr & values]
  (let [sym (gensym "f")]
    `(let [~sym (fn ~argv (assert ~expr))]
       (do ~@(map (fn [vs] `(~sym ~@vs))
                  (partition (count argv) values))))))

(def successful? t/success?)

(def run-tests t/run-spec)

(defn run-all-tests []
  (t/run-spec (all-ns)))

(defmacro thrown? [& args]
  `(t/thrown? ~@args))

(defmacro thrown-with-msg? [& args]
  `(t/thrown-with-msg? ~@args))
