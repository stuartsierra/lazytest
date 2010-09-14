(ns lazytest.dependency
  "Bidirectional graphs of dependencies and dependent objects."
  (:use [clojure.set :only (union)])
  (:refer-clojure :exclude (remove)))

(defn graph "Returns a new, empty, dependency graph." []
  {:dependencies {}
   :dependents {}})

(defn- transitive
  "Recursively expands the set of dependency relationships starting
  at (get m x)"
  [m x]
  (reduce (fn [s k]
	    (union s (transitive m k)))
	  (get m x) (get m x)))

(defn dependencies
  "Returns the set of all things x depends on, directly or transitively."
  [graph x]
  (transitive (:dependencies graph) x))

(defn dependents
  "Returns the set of all things which depend upon x, directly or
  transitively."
  [graph x]
  (transitive (:dependents graph) x))

(defn depends?
  "True if x is directly or transitively dependent on y."
  [graph x y]
  (contains? (dependencies graph x) y))

(defn dependent
  "True if y is a dependent of x."
  [graph x y]
  (contains? (dependents graph x) y))

(defn- add-relationship [graph key x y]
  (update-in graph [key x] union #{y}))

(defn depend
  "Adds to the dependency graph that x depends on deps.  Forbids
  circular dependencies."
  ([graph x dep]
     {:pre [(not (depends? graph dep x))]}
     (-> graph
	 (add-relationship :dependencies x dep)
	 (add-relationship :dependents dep x)))
  ([graph x dep & more]
     (apply depend
	    (depend graph x dep)
	    x more)))

(defn remove
  "Removes all references to x in the dependency graph."
  [graph x]
  (let [f (fn [amap]
	    (reduce (fn [m [k vs]]
		      (assoc m k (disj vs x)))
		    {} (dissoc amap x)))]
    (assoc graph
      :dependencies (f (:dependencies graph))
      :dependents (f (:dependents graph)))))
