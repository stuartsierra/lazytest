(ns lazytest.find
  (:use lazytest.suite
	lazytest.test-case))

(defn- find-var-test-value [this-var]
  {:pre [(var? this-var)]
   :post [(or (nil? %) (suite? %) (test-case? %))]}
  (when (bound? this-var)
    (let [value (var-get this-var)]
      (when (or (suite? value) (test-case? value))
	value))))

(defn- test-seq-for-ns [this-ns]
  (let [s (remove nil? (map find-var-test-value (vals (ns-interns this-ns))))]
    (when (seq s)
      (vary-meta s assoc :name (ns-name this-ns)))))

(defn find-ns-suite
  "Returns a test suite for the namespace.

  Returns nil if the namespace has no test suites.

  By default, recurses on all Vars in a namespace looking for values
  for which lazytest.suite/suite? is true.  If a namesapce
  has :test-suite metadata, uses that value instead.

  Always returns nil for the clojure.core namespace, to avoid special
  Vars such as *1, *2, *3"
  [n]
  {:post [(or (nil? %) (suite? %))]}
  (let [n (the-ns n)]
    (when-not (= (the-ns 'clojure.core) n)
      (or (:test-suite (meta n))
	  (when-let [s (test-seq-for-ns n)]
	    (suite (fn [] (test-seq s))))))))

(defn- suite-for-namespaces [names]
  (suite (with-meta (fn [] (test-seq (remove nil? (map find-ns-suite names))))
	   {:doc "Namespaces"})))

(def ^{:private true} all-ns-suite
     (suite
      (with-meta
	(fn [] (test-seq (remove nil? (map find-ns-suite (all-ns)))))
	{:doc "All Namespaces"})))

(defn find-suite
  "Returns test suite containing suites for the given namespaces.
  If no names given, searches all namespaces."
  [& names]
  (if (seq names)
    (suite-for-namespaces names)
    all-ns-suite))
