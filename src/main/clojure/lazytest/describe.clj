(ns lazytest.describe
  (:use	lazytest.expect
	lazytest.suite
	lazytest.find
	lazytest.test-case))

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

;;; Public API

(defmacro describe
  "Defines a group of tests.  

  decl is: sym? doc? attr-map? children*

  sym (optional) is a symbol; it will be resolved in the current namespace and
  prepended to the documentation string.

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  children are test cases (see 'it') or nested test groups"
  [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[attr-map children] (get-arg map? decl)
	docstring (strcat (when sym (resolve sym)) doc)
	metadata (merged-metadata children &form docstring attr-map)]
    `(def-unless-nested (suite (fn []
				 (with-meta
				   (flatten (list ~@children))
				   ~metadata))))))

(defmacro it
  "Defines a single test case.

  decl is: doc? attr-map? expr

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  expr is a single expression, which must return logical true to
  indicate the test case passes or logical false to indicate failure."
  [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[attr-map body] (get-arg map? decl)
	assertion (first body)
	metadata (merged-metadata body &form doc attr-map)]
    `(test-case (with-meta
		  (fn [] (expect ~assertion))
		  ~metadata))))

(defmacro do-it
  "Defines a single test case that may execute arbitrary code.

  decl is: doc? attr-map? body*

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  body is any code, which must throw an exception (such as with
  'expect') to indicate failure.  If the code completes without
  throwing any exceptions, the test case has passed."
  [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[attr-map body] (get-arg map? decl)
	metadata (merged-metadata body &form doc attr-map)]
    `(test-case (with-meta
		  (fn [] ~@body)
		  ~metadata))))
