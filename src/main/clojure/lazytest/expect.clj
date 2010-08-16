(ns lazytest.expect
  (:import (lazytest ExpectationFailed)))

(defn- local-bindings
  "Returns a map of the names of local bindings to their values."
  [env]
  (reduce (fn [m sym] (assoc m `'~sym sym))
	  {} (keys env)))

(defn- function-call?
  "True if form is a list representing a normal function call."
  [form]
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
  "Evaluates expression.  If it returns logical true, returns that
  result.  If the expression returns logical false, throws
  lazytest.ExpectationFailed with an attached map describing the
  reason for failure.  Metadata on expr and on the 'expect' form
  itself will be merged into the failure map."
  ([expr] `(expect nil ~expr))
  ([docstring expr]
     {:pre [(or (string? docstring) (nil? docstring))]}
     (if (function-call? expr)
       ;; Normal function call
       `(let [f# ~(first expr)
	      args# (list ~@(rest expr))
	      result# (apply f# args#)]
	  (or result#
	      (throw (ExpectationFailed.
		      (merge '~(meta &form)
			     '~(meta expr)
			     {:form '~expr
			      :locals ~(local-bindings &env)
			      :evaluated (list* f# args#)
			      :result result#
			      :file ~*file*
			      :ns *ns*}
			     ~(when docstring {:doc docstring}))))))
       ;; Unknown type of expression
       `(let [result# ~expr]
	  (or result#
	      (throw (ExpectationFailed.
		      (merge '~(meta &form)
			     '~(meta expr)
			     {:form '~expr
			      :locals ~(local-bindings &env)
			      :result result#
			      :file ~*file*
			      :ns *ns*}
			     ~(when docstring {:doc docstring})))))))))
