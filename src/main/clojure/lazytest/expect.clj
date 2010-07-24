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

(defn  expect-logical-true [value]
  (or value
      (throw (ExpectationFailed. (not-logical-true value)))))

(defn expect-predicate [pred & args]
  (or (apply pred args)
      (throw (ExpectationFailed. (predicate-failed pred args)))))

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

(defmacro expect-nothing-thrown [& body]
  `(do ~@body true))

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

(def
 ^{:doc "Map from predicate Vars to qualified symbols for expectation
 forms.  Used by the expect macro to convert logical tests into
 expectation expressions."}
 expectation
 {#'clojure.core/= `expect=
  #'clojure.core/not= `expect-not=
  #'clojure.core/instance? `expect-instance
  #'lazytest.expect/thrown? `expect-thrown
  #'lazytest.expect/thrown-with-msg? `expect-thrown-with-msg
  #'lazytest.expect/ok? `expect-nothing-thrown})

(defmacro expect
  "For each expression, does nothing if it returns logical true.  If
  the expression returns logical false, throws
  lazytest.ExpectationFailed with an attached object describing the
  reason for failure."  [& exprs]
  (list* `and
	 (map (fn [expr]
		(if (seq? expr)
		  (let [sym (first expr)
			args (rest expr)
			v (resolve sym)
			f (when (bound? v) (var-get v))
			expt (expectation v)]
		    (cond expt
			  (list* expt args)
			  (and f (fn? f) (not (:macro (meta v))))
			  `(expect-predicate ~f ~@args)
			  :else
			  expr))
		  expr))
	      exprs)))
