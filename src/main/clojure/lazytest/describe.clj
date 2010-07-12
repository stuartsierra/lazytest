(ns lazytest.describe
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable-test :only (RunnableTest run-tests
				       skip-or-pending try-expectations)]
	[lazytest.test-result :only (result-group)]
	[lazytest.fixture :only (setup teardown)]))

(defn get-arg
  "Pops first argument from args if (pred arg) is true.
  Returns a vector [this-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn get-options
  "Extracts keyword-value pairs from head of args, until next arg is
  not a keyword.  Returns a vector [options-map remaining-args]"
  [args]
  (loop [options nil args args]
    (if (and (seq args) (keyword? (first args)))
      (recur (assoc options (first args) (second args)) (nnext args))
      [options args])))

(defn firsts [coll]
  (take-nth 2 coll))

(defn seconds [coll]
  (take-nth 2 (drop 1 coll)))

(defn wrap-in-fns [forms]
  (map (fn [f] `(fn [] ~f)) forms))

(defn get-tests-with-parent [parent t]
  (get-tests
   (assoc t
     :locals (vec (concat (:locals parent) (:locals t)))
     :fixtures (vec (concat (:fixtures parent) (:fixtures t))))))

(defrecord DescribeRunnable [children]
  RunnableTest
  (run-tests [this]
	     (or (skip-or-pending this)
		 (result-group this (mapcat run-tests children)))))

(defrecord Describe [locals fixtures children-thunk]
  Testable
  (get-tests [this]
	     (list
	      (DescribeRunnable.
	       (mapcat (partial get-tests-with-parent this)
		       (children-thunk))
	       (meta this)
	       nil))))

(defmacro describe [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[opts body] (get-options decl)
	;; argument parsing done
	givens (:given opts)
	locals (vec (firsts givens))
	Fixtures ( (wrap-in-fns (seconds givens)))
	metadata (merge (meta &form)
			(dissoc opts :given)
			(when (empty? body) {:pending true}))
	group-sym (gensym "describe-")]
    `(Describe. '~locals ~fixtures (fn [] (vector ~@body)))))

(defn setup-fixture [thunk]
  (let [value (thunk)]
    (if (fixture? value)
      (setup value)
      value)))

(defrecord ItRunnable [fixtures f]
  RunnableTest
  (run-tests [this]
	     (list (or (skip-or-pending this)
		       (let [result (try-expectations
				     (apply f (map setup (map #(%) fixtures))))]
			 (dorun (map teardown fixtures))
			 result)))))

(defrecord It [locals fixtures body]
  Testable
  (get-tests [this]
	     (list (ItRunnable. fixtures
				(eval `(fn ~locals ~@body))
				(meta this)
				nil))))

(defmacro it [& decl]
  (let [[doc decl] (get-arg string? decl)
	[opts body] (get-options decl)
	;; argument parsing done
	givens (:given opts)
	locals (vec (firsts givens))
	fixtures (vec (wrap-in-fns (seconds givens)))
	metadata (merge (meta &form)
			(dissoc opts :given)
			(when (empty? body) {:pending true}))]
    `(It. '~locals ~fixtures '~body)))
