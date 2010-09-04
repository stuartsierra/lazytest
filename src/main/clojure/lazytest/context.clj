(ns lazytest.context
  "Contexts provide context and/or state in which to run one or more
  tests.")

(defprotocol Context
  (setup [this] "Creates and returns state provided by this context.")
  (teardown [this] "Cleans up state created by this context."))

(defn context?
  "Returns true if x is a context."
  [x]
  (extends? Context (type x)))

(deftype FunctionContext [setup-fn teardown-fn]
  Context
  (setup [this] (setup-fn))
  (teardown [this] (teardown-fn)))

(defn fn-context
  "Creates a context with the given setup and teardown functions."
  [setup-fn teardown-fn]
  {:pre [(fn? setup-fn) (fn? teardown-fn)]}
  (FunctionContext. setup-fn teardown-fn))

(defn add-context
  "Adds context c to metadata of iobj."
  ([iobj c]
     {:pre [(context? c)]}
     (vary-meta iobj update-in [::contexts] conj c))
  ([iobj c & more]
     (reduce add-context (add-context iobj c) more)))

(defn setup-contexts
  "Run setup functions of all contexts in iobj's metadata."
  [iobj]
  (doseq [c (::contexts (meta iobj))]
    (setup c)))

(defn teardown-contexts
  "Run teardown functions of all contexts in iobj's metadata."
  [iobj]
  (doseq [c (reverse (::contexts (meta iobj)))]
    (teardown c)))
