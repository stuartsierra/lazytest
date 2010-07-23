(ns lazytest.describe
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable-test :only (RunnableTest run-tests
				       skip-or-pending try-expectations)]
	[lazytest.group :only (test-case test-group)]
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

(defn- strcat
  "Concatenate strings, with spaces in between, skipping nil."
  [& args]
  (apply str (interpose \space (remove nil? args))))

(defmacro describe [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[opts body] (get-options decl)
	children (vec body)
	metadata (merge (meta &form) {:doc doc} opts)]
    `(test-group ~children ~metadata)))

(defmacro given [& decl]
  (let [[doc decl] (get-arg string? decl)
	[opts decl] (get-options decl)
	[bindings body] (get-options vector? decl)
	children (vec body)
	metadata (merge (meta &form) {:doc doc} opts)]
    `(test-group ~children ~metadata)))

(defmacro using [& decl]
  (let [[doc decl] (get-arg string? decl)
	[opts decl] (get-options decl)
	[bindings body] (get-options vector? decl)
	children (vec body)
	metadata (merge (meta &form) {:doc doc} opts)]
    `(test-group ~children ~metadata)))

(defmacro it [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[opts body] (get-options decl)
	metadata (merge (meta &form) {:doc doc} opts)]
    `(test-case [] (fn [] ~@body) ~metadata)))
