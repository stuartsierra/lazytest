(ns expect-test)

(defn foo
  ([x y]
     (+ x y))
  {:expectations [{:input [3 4] :output 7}
		  {:input [2 2] :output 4}]})

(defn bar
  ([a]
     (* 100 a))
  {:expectations [{:input [5] :output 500}]})

(defn munge
  ([s] (reverse s))
  {:expectations [{:input [[1 2 3 4]] :output [4 3 2 1]}]})


(require 'lazytest.adapter.expectation-meta)
(lazytest.adapter.expectation-meta/set-suite-for-ns!)

