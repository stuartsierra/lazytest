(ns lazytest.find
  (:use lazytest.suite))

(defn- find-suite-for-var [this-var]
  {:pre [(var? this-var)]
   :post [(or (nil? %) (suite? %))]}
  (when (bound? this-var)
    (let [value (var-get this-var)]
      (when (suite? value)
	value))))

(defn- test-seq-for-ns [this-ns]
  (let [s (remove nil? (map find-suite-for-var (vals (ns-interns this-ns))))]
    (when (seq s)
      (vary-meta s assoc :name (ns-name this-ns)))))

(defn- find-suite-for-namespace [this-ns]
  {:pre [(instance? clojure.lang.Namespace this-ns)]
   :post [(or (nil? %) (suite? %))]}
  (when-not (= (the-ns 'clojure.core) this-ns)
    (or (:test-suite (meta this-ns))
	(when-let [s (test-seq-for-ns this-ns)]
	  (suite (fn [] (test-seq s)))))))

(defn find-suite
  "Returns a test suite for x, which may be a Var, a namespace, or a
  symbol naming a loaded namespace.

  Returns nil if x has no test suites.

  By default, recurses on all Vars in a namespace looking for values
  for which lazytest.suite/suite? is true.  If a namesapce
  has :test-suite metadata, uses that value instead.

  Always returns nil for the clojure.core namespace, to avoid special
  Vars such as *1, *2, *3"
  [x]
  (cond (symbol? x)
	  (find-suite-for-namespace (the-ns x))
	(instance? clojure.lang.Namespace x)
	  (find-suite-for-namespace x)
	(var? x)
	  (find-suite-for-var x)
        :else nil))

(defn find-suites
  "Returns a sequence of test suites found in the named namespaces.
  If no names given, searches all namespaces."
  [& names]
  (seq (remove nil? (map find-suite (or (seq names) (all-ns))))))
