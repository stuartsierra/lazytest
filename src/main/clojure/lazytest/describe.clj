(ns lazytest.describe
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable-test :only (RunnableTest run-tests
				       skip-or-pending try-expectations)]
	[lazytest.test-result :only (result-group)]
	[lazytest.expect :only (expect)]
	[lazytest.fixture :only (setup teardown)]))

;;; Utilities

(defn- get-arg
  "Pops first argument from args if (pred arg) is true.
  Returns a vector [this-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn- get-options
  "Extracts keyword-value pairs from head of args, until next arg is
  not a keyword.  Returns a vector [options-map remaining-args]"
  [args]
  (loop [options nil args args]
    (if (and (seq args) (keyword? (first args)))
      (recur (assoc options (first args) (second args)) (nnext args))
      [options args])))

(defn- firsts [coll]
  (take-nth 2 coll))

(defn- seconds [coll]
  (take-nth 2 (drop 1 coll)))

(defn- thunk-forms [forms]
  (map (fn [f] `(fn [] ~f)) forms))

(defn- get-child-tests [parent t]
  (get-tests
   (assoc t
     :locals (vec (concat (:locals parent) (:locals t)))
     :fixtures (vec (concat (:fixtures parent) (:fixtures t))))))

;;; Core

(defrecord RunnableExample [fixtures f]
  RunnableTest
  (run-tests [this]
	     (list (try-expectations this
		    (apply f (map setup fixtures))
		    (dorun (map teardown fixtures))))))

(defrecord DescribeGroup [children]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (or (skip-or-pending this)
		 (result-group this (mapcat run-tests children)))))

(defmacro describe [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[opts body] (get-options decl)
	children (vec body)
	metadata (merge (meta &form) opts)]
    `(DescribeGroup. ~body '~metadata nil)))

(defrecord ThenGroup [assertions]
  Testable
  (get-tests [this] (list this))
  RunnableTest
  (run-tests [this]
	     (or (skip-or-pending this)
		 (try-expectations this (dorun (map #(%) assertions)))))) 

(defmacro then [& decl]
  (let [[doc decl] (get-arg string? decl)
	[opts body] (get-options decl)
	metadata (merge (meta &form) opts)
	assertions (map (fn [assertion] `(fn [] (expect ~assertion)))
			body)]
    `(ThenGroup. ~assertions '~metadata nil)))
