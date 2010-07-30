(ns lazytest.expect
  (:import (lazytest ExpectationFailed)))

(defmacro thrown?
  "Returns true if body throws an instance of class c."
  [c & body]
  `(try ~@body false
        (catch ~c e# true)))

(defmacro thrown-with-msg?
  "Returns true if body throws an instance of class c whose message
  matches re (with re-find)."
  [c re & body]
  `(try ~@body false
        (catch ~c e# (re-find ~re (.getMessage e#)))))

(defmacro ok?
  "Returns true if body does not throw anything."
  [& body]
  `(do ~@body true))

(defn function-call? [form]
  (and (seq? form)
       (let [sym (first form)]
	 (and (symbol? sym)
	      (let [v (resolve sym)]
		(and (var? v)
		     (bound? v)
		     (not (:macro (meta v)))
		     (let [f (var-get v)]
		       (fn? f))))))))

(defmacro expect
  "For each expression, does nothing if it returns logical true.  If
  the expression returns logical false, throws
  lazytest.ExpectationFailed with an attached object describing the
  reason for failure."  [expr]
  (if (function-call? expr)
    `(let [f# ~(first expr)
	   args# (list ~@(rest expr))
	   result# (apply f# args#)]
       ;; can't use if-let b/c the binding doesn't include else clause
       (or result#
	   (throw (ExpectationFailed. {:form '~expr
				       :evaluated (list* f# args#)
				       :result result#}))))
    `(let [result# ~expr]
       (or result#
	   (throw (ExpectationFailed. {:form '~expr
				       :result result#}))))))
