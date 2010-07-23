(ns lazytest.testable)

(defprotocol Testable
  (get-tests [this]
"Returns a seq of RunnableTest objects. Processes the :focus metadata flag.
 Default implementation recurses on all Vars in a namespace, unless
 that namespace has :get-tests metadata."))

(defn focused?
  "True if x has :focus metadata set to true."
  [x]
  (boolean (:focused (meta x))))

(defn filter-focused
  "If any items in sequence s are focused, return them; else return s."
  [s]
  (or (seq (filter focused? s)) s))

(extend-protocol Testable
  clojure.lang.Namespace
  (get-tests [this-ns]
    (if-let [f (:get-tests (meta this-ns))]
      (do (assert (fn? f)) (f))
      (filter-focused
       (mapcat get-tests (vals (ns-interns this-ns))))))

  clojure.lang.Var
  (get-tests [this-var]
    (when (bound? this-var)
      (let [value (var-get this-var)]
	(when (extends? Testable (type value))
	  (filter-focused
	   (get-tests value)))))))
