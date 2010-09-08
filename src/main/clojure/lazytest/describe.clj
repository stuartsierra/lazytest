(ns lazytest.describe
  (:use	lazytest.expect
	lazytest.suite
	lazytest.find
	lazytest.test-case
	lazytest.context))

;;; Utilities

(defn- get-arg
  "Pops first argument from args if (pred arg) is true.
  Returns a vector [this-arg remaining-args] or [nil args]."
  [pred args]
  (if (pred (first args))
    [(first args) (next args)]
    [nil args]))

(defn- merged-metadata [body form docstring extra-attr-map]
  (merge (when (empty? body) {:pending true})
	 {:doc docstring, :file *file*, :ns *ns*}
	 (meta form)
	 extra-attr-map))

(defn- strcat
  "Concatenate strings, with spaces in between, skipping nil."
  [& args]
  (apply str (interpose \space (remove nil? args))))

;;; Public API

(defmacro testing
  "Like 'describe' but does not create a Var.  Used for nesting test
  suites inside 'describe'."
  [& decl]
  (let [[sym decl] (get-arg symbol? decl)
	[doc decl] (get-arg string? decl)
	[attr-map children] (get-arg map? decl)
	docstring (strcat (when sym (resolve sym)) doc)
	metadata (merged-metadata children &form docstring attr-map)]
    `(suite (fn []
	      (test-seq
	       (with-meta
		 (flatten (list ~@children))
		 ~metadata))))))

(defmacro describe
  "Defines a suite of tests assigned to a Var with a generated name.
  Evaluating the same 'describe' form multiple times yields multiple
  Vars with different names.

  decl is: sym? doc? attr-map? children*

  sym (optional) is a symbol; it will be resolved in the current namespace and
  prepended to the documentation string.

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  children are test cases (see 'it') or nested test suites (see 'testing')"
  [& decl]
  `(def ~(gensym) (testing ~@decl)))

(defmacro with
  "Adds a collection of contexts to each of the body expressions."
  [contexts & body]
  {:pre [(coll? contexts)]}
  `(let [contexts# ~contexts]   ;; avoid duplicate evaluation
     (map (fn [x#] (apply add-context x# contexts#)) (flatten (list ~@body)))))

(defmacro using
  "bindings is a vector of name-value pairs, like 'given', but each
  value is a context. Adds contexts to each of the body expressions,
  where they may be dereferenced by the given names."
  [bindings & body]
  {:pre [(vector? bindings)
	 (even? (count bindings))]}
  `(given ~bindings
     (with ~(vec (take-nth 2 (drop 1 bindings)))
	   (list ~@body))))

(defmacro before
  "Returns a context whose teardown function evaluates body."
  [& body]
  `(fn-context (fn [] ~@body) (constantly nil)))

(defmacro after
  "Returns a context whose teardown method evaluates body."
  [& body]
  `(fn-context (constantly nil) (fn [] ~@body)))

(defmacro it
  "Defines a single test case.

  decl is: doc? attr-map? expr

  doc (optional) is a documentation string

  attr-map (optional) is a metadata map

  expr is a single expression, which must return logical true to
  indicate the test case passes or logical false to indicate failure."
  [& decl]
  (let [[doc decl] (get-arg string? decl)
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
  (let [[doc decl] (get-arg string? decl)
	[attr-map body] (get-arg map? decl)
	metadata (merged-metadata body &form doc attr-map)]
    `(test-case (with-meta
		  (fn [] ~@body)
		  ~metadata))))

(defmacro given
  "Like 'let' but returns the expressions of body in a list.
  Suitable for nesting inside 'describe' or 'testing'."
  [bindings & body]
  {:pre [(vector? bindings)
	 (even? (count bindings))]}
  `(let ~bindings (list ~@body)))
