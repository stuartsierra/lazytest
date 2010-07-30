(ns lazytest.describe
  (:use [lazytest.testable :only (Testable get-tests)]
	[lazytest.runnable-test :only (RunnableTest run-tests
				       skip-or-pending)]
	[lazytest.group :only (test-case test-group)]
	[lazytest.result :only (result-group)]
	[lazytest.expect :only (expect)]
	[lazytest.fixture :only (setup teardown function-fixture)]))

;;; Utilities

(defn- get-arg
  "Pops first argument from args if (pred arg) is true.
  Returns a vector [this-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

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

(defn- merged-metadata [body form docstring extra-attr-map]
  (merge (when (empty? body) {:pending true})
	 {:doc docstring, :file *file*, :ns *ns*}
	 (meta form)
	 extra-attr-map))

;;; Local Variable Scope

(let [counter (atom 0)]
  (defn- local-counter []
    (swap! counter inc)))

(defmacro bind-local
  "Establishes a local binding of sym to the state returned by local-scope
  c in all groups and examples found within body."
  [form c & body]
  `(let [~(with-meta (gensym) {::local true
			       ::order (local-counter)
			       ::arg form}) ~c]
     ~@body))

(defmacro wrap-local-scope
  "Recursively creates local bindings for each pair in bindings vector."
  [bindings body]
  {:pre [(or (vector? bindings) (nil? bindings))
         (even? (count bindings))]}
  (if (seq bindings)
    `(bind-local ~(first bindings) ~(second bindings)
       (wrap-local-scope ~(vec (nnext bindings)) ~body))
    body))

(defn- find-locals
  "Returns a vector of locals bound by bind-local in the
  environment."
  [env]
  (vec (sort-by #(::order (meta %))
                (filter #(::local (meta %)) (keys env)))))

(defn- find-local-binding-forms
  "Returns a vector of the function argument forms of locals bound by
  bind-local in the environment."
  [env]
  (vec (map #(::arg (meta %)) (find-locals env))))

;;; Top-level definitions

(defmacro def-unless-nested [form]
  (if (some #(::nested (meta %)) (keys &env))
    form
    `(let [~(with-meta (gensym) {::nested true}) nil]
       (def ~(gensym) ~form))))

;;; Public

(defmacro describe [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[attr-map body] (get-arg map? decl)
	children (vec body)
	docstring (strcat (when sym (resolve sym)) doc)
	metadata (merged-metadata body &form docstring attr-map)]
    `(def-unless-nested (test-group ~children ~metadata))))

(defmacro given [& decl]
  (let [[doc decl] (get-arg string? decl)
	[attr-map decl] (get-arg map? decl)
	[bindings body] (get-arg vector? decl)
	children (vec body)
	metadata (merged-metadata body &form doc attr-map)]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [binding-forms (firsts bindings)
	  fixtures (map (fn [x] `(function-fixture (fn ~(find-locals &env) ~x)))
			(seconds bindings))
	  local-bindings (vec (interleave binding-forms fixtures))]      
      `(wrap-local-scope ~local-bindings (test-group ~children ~metadata)))))

(defmacro using [& decl]
  (let [[doc decl] (get-arg string? decl)
	[attr-map decl] (get-arg map? decl)
	[bindings body] (get-arg vector? decl)
	children (vec body)
	metadata (merged-metadata body &form doc attr-map)]
    (assert (vector? bindings))
    (assert (even? (count bindings)))
    (let [binding-forms (firsts bindings)
	  fixtures (seconds bindings)
	  local-bindings (vec (interleave binding-forms fixtures))]      
      `(wrap-local-scope ~local-bindings (test-group ~children ~metadata)))))

(defmacro it [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[attr-map body] (get-arg map? decl)
	assertion (first body)
	metadata (merged-metadata body &form doc attr-map)]
    `(test-case ~(find-locals &env)
		(fn ~(find-local-binding-forms &env) (expect ~assertion))
		~metadata)))

(defmacro do-it [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[attr-map body] (get-arg map? decl)
	metadata (merged-metadata body &form doc attr-map)]
    `(test-case ~(find-locals &env)
		(fn ~(find-local-binding-forms &env) ~@body) ~metadata)))
