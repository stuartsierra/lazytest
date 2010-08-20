(ns lazytest.find)

(defn- find-tests-for-var [this-var]
  (if-let [f (:find-tests (meta this-var))]
    (do (assert (fn? f)) (f))
    (when (bound? this-var)
      (let [value (var-get this-var)]
	(if (:lazytest/suite (meta value))
	  value)))))

(defn- find-tests-in-namespace [this-ns]
  (when-not (= (the-ns 'clojure.core) this-ns)
    (if-let [f (:find-tests (meta this-ns))]
      (do (assert (fn? f)) (f))
      (map find-tests-for-var (vals (ns-interns this-ns))))))

(defn find-tests [x]
  "Returns a seq of runnable tests. Processes the :focus metadata flag.
 Recurses on all Vars in a namespace, unless that namespace
 has :find-tests metadata."
  (cond (symbol? x)
	  (find-tests (the-ns x))
	(instance? clojure.lang.Namespace x)
	  (find-tests-in-namespace x)
	(var? x)
	  (find-tests-for-var x)
        :else nil))
