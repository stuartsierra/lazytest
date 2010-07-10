(ns lazytest.expect
  (:use lazytest.failure)
  (:import (lazytest ExpectationFailed)))

(defn expect= [& args]
  (or (apply = args)
      (throw (ExpectationFailed. (not-equal args)))))

(defn expect-not= [& args]
  (or (apply not= args)
      (throw (ExpectationFailed. (not-not-equal args)))))

(defn expect-instance [class object]
  (or (instance? class object)
      (throw (ExpectationFailed. (not-instance-of class object)))))

(defmacro expect-thrown [class & body]
  `(try ~@body
	(throw (ExpectationFailed. (not-thrown ~class)))
	(catch ~class e# true)))

(defmacro expect-thrown-with-msg [class re & body]
  `(try ~@body
	(throw (ExpectationFailed. (not-thrown ~class)))
	(catch ~class e#
	  (if (re-find ~re (.getMessage e#))
	    true
	    (throw (ExpectationFailed. (thrown-with-wrong-message ~re (.getMessage e#))))))))

(defn  expect-logical-true [value]
  (or value
      (throw (ExpectationFailed. (not-logical-true value)))))

(defn expect-predicate [pred args]
  (or (apply pred args)
      (throw (ExpectationFailed. (predicate-failed pred args)))))

(def expectation
     {#'clojure.core/= `expect=
      #'clojure.core/not= `expect-not=
      #'clojure.core/instance? `expect-instance})

(defmacro expect [expr]
  (if (seq? expr)
    (let [sym (first expr)
	  args (rest expr)
	  v (resolve sym)
	  f (when (bound? v) (var-get v))
	  expt (expectation v)]
      (cond expt
	      (list* expt args)
	    (and f (fn? f) (not (:macro (meta v))))
	      (expect-predicate f args)
	    :else
	      expr))
    expr))
