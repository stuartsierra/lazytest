(ns lazytest.find)

(defprotocol FindTests
  (find-tests [this]
"Returns a seq of RunnableTest objects. Processes the :focus metadata flag.
 Default implementation recurses on all Vars in a namespace, unless
 that namespace has :find-tests metadata."))

(defn focused?
  "True if x has :focus metadata set to true."
  [x]
  (boolean (:focused (meta x))))

(defn filter-focused
  "If any items in sequence s are focused, return them; else return s."
  [s]
  (or (seq (filter focused? s)) s))

(extend-protocol FindTests
  clojure.lang.Namespace
  (find-tests [this-ns]
    (when-not (= (the-ns 'clojure.core) this-ns)
      (if-let [f (:find-tests (meta this-ns))]
	(do (assert (fn? f)) (f))
	(filter-focused
	 (mapcat find-tests (vals (ns-interns this-ns)))))))

  clojure.lang.Var
  (find-tests [this-var]
    (if-let [f (:find-tests (meta this-var))]
      (do (assert (fn? f)) (f))
      (when (bound? this-var)
	(let [value (var-get this-var)]
	  (when (extends? FindTests (type value))
	    (filter-focused
	     (find-tests value))))))))
