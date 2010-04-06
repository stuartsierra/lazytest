(ns foo
  (:use com.stuartsierra.lazytest))

(defn foo [] true)

(describe *ns* "The foo namespace"
          (spec "has a function foo"
                (is (foo))))
