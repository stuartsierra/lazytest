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

(defn causes
  "Given a Throwable, returns a sequence of the Throwables that caused
  it, in the order they occurred."
  [throwable]
  (lazy-seq
   (when throwable
     (cons throwable (causes (.getCause throwable))))))

(defmacro caused?
  "Returns true if body throws an exception which is of class c or
  caused by an exception of class c."
  [c & body]
  `(try ~@body false
	(catch Throwable e#
	  (if (some (fn [cause#] (instance? ~c cause#)) (causes e#))
	    true
	    (throw e#)))))

(defmacro caused-with-msg?
  "Returns true if body throws an exception of class c or caused by
  an exception of class c whose message matches re (with re-find)."
  [c re & body]
  `(try ~@body false
	(catch Throwable e#
	  (if (some (fn [cause#]
		      (and (instance? ~c cause#)
			   (re-find ~re (.getMessage cause#))))
		    (causes e#))
	    true
	    (throw e#)))))

(defmacro ok?
  "Returns true if body does not throw anything."
  [& body]
  `(do ~@body true))

(defn- function-call? [form]
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
			      :result result#
			      :file ~*file*
			      :ns *ns*}
			     ~(when docstring {:doc docstring})))))))))
